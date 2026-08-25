import { IsNotEmpty, IsString } from 'class-validator';

export class LoginDto {
  @IsNotEmpty({ message: 'Email or username is required' })
  @IsString()
  login: string; // Accepts either email or username

  @IsNotEmpty({ message: 'Password is required' })
  @IsString()
  password: string;

  @IsString()
  device_info?: string;
}
