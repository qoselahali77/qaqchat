import { IsEmail, IsNotEmpty, IsOptional, IsString, Length, Matches } from 'class-validator';

export class RegisterDto {
  @IsNotEmpty({ message: 'First name is required' })
  @IsString()
  @Length(1, 64, { message: 'First name must be between 1 and 64 characters' })
  first_name: string;

  @IsNotEmpty({ message: 'Last name is required' })
  @IsString()
  @Length(1, 64, { message: 'Last name must be between 1 and 64 characters' })
  last_name: string;

  @IsNotEmpty({ message: 'Username is required' })
  @IsString()
  @Length(3, 32, { message: 'Username must be between 3 and 32 characters' })
  @Matches(/^[a-zA-Z0-9_]+$/, { message: 'Username can only contain letters, numbers, and underscores' })
  username: string;

  @IsNotEmpty({ message: 'Email is required' })
  @IsEmail({}, { message: 'Please provide a valid email address' })
  email: string;

  @IsNotEmpty({ message: 'Password is required' })
  @IsString()
  @Length(6, 64, { message: 'Password must be at least 6 characters long' })
  password: string;

  @IsOptional()
  @IsString()
  display_name?: string;
}
