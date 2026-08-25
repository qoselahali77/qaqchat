import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User, UserStatus } from './entities/user.entity';
import { UpdateUserDto } from './dto/update-user.dto';

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
  ) {}

  async findById(id: string): Promise<User> {
    const user = await this.userRepository.findOne({ where: { id } });
    if (!user) {
      throw new NotFoundException(`User with ID ${id} not found`);
    }
    return user;
  }

  async findByUsername(username: string): Promise<User | null> {
    return this.userRepository.findOne({ where: { username } });
  }

  async findByEmail(email: string): Promise<User | null> {
    return this.userRepository.findOne({ where: { email } });
  }

  async findByGoogleId(googleId: string): Promise<User | null> {
    return this.userRepository.findOne({ where: { google_id: googleId } });
  }

  async findByLogin(login: string): Promise<User | null> {
    return this.userRepository
      .createQueryBuilder('user')
      .addSelect('user.password_hash')
      .where('user.email = :login OR user.username = :login', { login })
      .getOne();
  }

  async create(data: {
    username: string;
    email: string;
    password_hash?: string | null;
    first_name?: string | null;
    last_name?: string | null;
    display_name: string;
    avatar_url?: string | null;
    google_id?: string | null;
  }): Promise<User> {
    const existingUsername = await this.findByUsername(data.username);
    if (existingUsername) {
      throw new ConflictException('Username is already taken');
    }

    const existingEmail = await this.findByEmail(data.email);
    if (existingEmail) {
      throw new ConflictException('Email is already registered');
    }

    const user = this.userRepository.create({
      ...data,
      status: UserStatus.OFFLINE,
    });

    return this.userRepository.save(user);
  }

  async update(id: string, updateDto: UpdateUserDto): Promise<User> {
    const user = await this.findById(id);
    Object.assign(user, updateDto);
    return this.userRepository.save(user);
  }

  async updateStatus(id: string, status: UserStatus): Promise<void> {
    await this.userRepository.update(id, { status });
  }
}
