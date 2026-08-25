import { Injectable, UnauthorizedException } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {
  handleRequest(err, user, info) {
    if (err || !user) {
      throw err || new UnauthorizedException('Authentication required to access this resource');
    }
    if (user.is_banned) {
      throw new UnauthorizedException('Your account has been suspended');
    }
    return user;
  }
}
