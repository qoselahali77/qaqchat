import {
  Injectable,
  UnauthorizedException,
  BadRequestException,
  Logger,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import { OAuth2Client } from 'google-auth-library';
import { UsersService } from '../users/users.service';
import { AuthSession } from './entities/auth-session.entity';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { RefreshTokenDto } from './dto/refresh-token.dto';
import { GoogleAuthDto } from './dto/google-auth.dto';
import { User, UserStatus } from '../users/entities/user.entity';

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);
  private readonly googleClient = new OAuth2Client();

  constructor(
    private readonly usersService: UsersService,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
    @InjectRepository(AuthSession)
    private readonly sessionRepository: Repository<AuthSession>,
  ) {}

  async register(registerDto: RegisterDto) {
    const salt = await bcrypt.genSalt(10);
    const password_hash = await bcrypt.hash(registerDto.password, salt);

    const displayName =
      registerDto.display_name?.trim() ||
      `${registerDto.first_name.trim()} ${registerDto.last_name.trim()}`;

    const user = await this.usersService.create({
      username: registerDto.username.trim(),
      email: registerDto.email.trim(),
      first_name: registerDto.first_name.trim(),
      last_name: registerDto.last_name.trim(),
      display_name: displayName,
      password_hash,
    });

    const tokens = await this.generateTokens(user);
    await this.saveSession(user.id, tokens.refresh_token, 'Android Device');

    await this.usersService.updateStatus(user.id, UserStatus.ONLINE);
    user.status = UserStatus.ONLINE;

    return {
      user,
      tokens,
    };
  }

  async login(loginDto: LoginDto, ipAddress?: string) {
    const user = await this.usersService.findByLogin(loginDto.login);
    if (!user) {
      throw new UnauthorizedException('Invalid credentials');
    }

    if (user.is_banned) {
      throw new UnauthorizedException('This account has been suspended');
    }

    if (!user.password_hash) {
      throw new UnauthorizedException(
        'This account was registered via Google Sign-In. Please sign in with Google.',
      );
    }

    const isMatch = await bcrypt.compare(loginDto.password, user.password_hash);
    if (!isMatch) {
      throw new UnauthorizedException('Invalid credentials');
    }

    const tokens = await this.generateTokens(user);
    await this.saveSession(
      user.id,
      tokens.refresh_token,
      loginDto.device_info || 'Android Client',
      ipAddress,
    );

    await this.usersService.updateStatus(user.id, UserStatus.ONLINE);
    user.status = UserStatus.ONLINE;

    delete user.password_hash;

    return {
      user,
      tokens,
    };
  }

  async googleAuth(googleAuthDto: GoogleAuthDto, ipAddress?: string) {
    try {
      // 1. Verify Google ID token
      const ticket = await this.googleClient.verifyIdToken({
        idToken: googleAuthDto.id_token,
      });
      const payload = ticket.getPayload();
      if (!payload || !payload.email) {
        throw new UnauthorizedException('Invalid Google ID Token');
      }

      const googleId = payload.sub;
      const email = payload.email;
      const firstName = payload.given_name || 'User';
      const lastName = payload.family_name || '';
      const displayName = payload.name || `${firstName} ${lastName}`.trim();
      const avatarUrl = payload.picture || null;

      // 2. Find existing user by google_id or by email
      let user = await this.usersService.findByGoogleId(googleId);
      if (!user) {
        user = await this.usersService.findByEmail(email);
        if (user) {
          // Link existing email account to Google
          user.google_id = googleId;
          if (!user.avatar_url && avatarUrl) {
            user.avatar_url = avatarUrl;
          }
          await this.usersService.update(user.id, {
            avatar_url: user.avatar_url,
          });
        } else {
          // Create new user with a unique username based on email
          let baseUsername = email.split('@')[0].replace(/[^a-zA-Z0-9_]/g, '_');
          if (baseUsername.length < 3) baseUsername = `user_${baseUsername}`;
          baseUsername = baseUsername.substring(0, 24);

          let finalUsername = baseUsername;
          let counter = 1;
          while (await this.usersService.findByUsername(finalUsername)) {
            finalUsername = `${baseUsername}_${counter++}`;
          }

          user = await this.usersService.create({
            username: finalUsername,
            email,
            first_name: firstName,
            last_name: lastName,
            display_name: displayName,
            avatar_url: avatarUrl,
            google_id: googleId,
          });
        }
      }

      if (user.is_banned) {
        throw new UnauthorizedException('This account has been suspended');
      }

      const tokens = await this.generateTokens(user);
      await this.saveSession(
        user.id,
        tokens.refresh_token,
        googleAuthDto.device_info || 'Android Google Sign-In',
        ipAddress,
      );

      await this.usersService.updateStatus(user.id, UserStatus.ONLINE);
      user.status = UserStatus.ONLINE;

      return {
        user,
        tokens,
      };
    } catch (err) {
      if (err instanceof UnauthorizedException) throw err;
      this.logger.error(`Google auth error: ${err.message}`, err.stack);
      throw new UnauthorizedException('Failed to authenticate with Google');
    }
  }

  async refreshToken(dto: RefreshTokenDto) {
    try {
      const refreshSecret = this.configService.get<string>('jwt.refreshSecret');
      const payload = this.jwtService.verify(dto.refresh_token, {
        secret: refreshSecret,
      });

      const sessions = await this.sessionRepository.find({
        where: { user_id: payload.sub },
      });

      let matchedSession: AuthSession | null = null;
      for (const session of sessions) {
        if (new Date() > session.expires_at) {
          await this.sessionRepository.remove(session);
          continue;
        }

        const isMatch = await bcrypt.compare(
          dto.refresh_token,
          session.refresh_token_hash,
        );
        if (isMatch) {
          matchedSession = session;
          break;
        }
      }

      if (!matchedSession) {
        throw new UnauthorizedException('Invalid or expired refresh token');
      }

      const user = await this.usersService.findById(payload.sub);
      if (!user || user.is_banned) {
        throw new UnauthorizedException('User inactive or suspended');
      }

      const newTokens = await this.generateTokens(user);

      const salt = await bcrypt.genSalt(10);
      matchedSession.refresh_token_hash = await bcrypt.hash(
        newTokens.refresh_token,
        salt,
      );
      matchedSession.expires_at = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
      await this.sessionRepository.save(matchedSession);

      return {
        tokens: newTokens,
      };
    } catch (err) {
      throw new UnauthorizedException('Session expired, please log in again');
    }
  }

  async logout(userId: string, refreshToken?: string) {
    if (refreshToken) {
      const sessions = await this.sessionRepository.find({
        where: { user_id: userId },
      });
      for (const session of sessions) {
        const isMatch = await bcrypt.compare(
          refreshToken,
          session.refresh_token_hash,
        );
        if (isMatch) {
          await this.sessionRepository.remove(session);
          break;
        }
      }
    } else {
      await this.sessionRepository.delete({ user_id: userId });
    }

    await this.usersService.updateStatus(userId, UserStatus.OFFLINE);
    return { success: true, message: 'Logged out successfully' };
  }

  private async generateTokens(user: User) {
    const payload = {
      sub: user.id,
      username: user.username,
      email: user.email,
    };

    const accessSecret = this.configService.get<string>('jwt.accessSecret');
    const accessExpiresIn = this.configService.get<string>('jwt.accessExpiresIn');
    const refreshSecret = this.configService.get<string>('jwt.refreshSecret');
    const refreshExpiresIn = this.configService.get<string>('jwt.refreshExpiresIn');

    const [access_token, refresh_token] = await Promise.all([
      this.jwtService.signAsync(payload, {
        secret: accessSecret,
        expiresIn: accessExpiresIn,
      }),
      this.jwtService.signAsync(payload, {
        secret: refreshSecret,
        expiresIn: refreshExpiresIn,
      }),
    ]);

    return {
      access_token,
      refresh_token,
      expires_in: 15 * 60,
    };
  }

  private async saveSession(
    userId: string,
    refreshToken: string,
    deviceInfo?: string,
    ipAddress?: string,
  ) {
    const salt = await bcrypt.genSalt(10);
    const refresh_token_hash = await bcrypt.hash(refreshToken, salt);
    const expires_at = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);

    const session = this.sessionRepository.create({
      user_id: userId,
      refresh_token_hash,
      device_info: deviceInfo,
      ip_address: ipAddress,
      expires_at,
    });

    await this.sessionRepository.save(session);
  }
}
