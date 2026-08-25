import { IsNotEmpty, IsOptional, IsString } from 'class-validator';

export class GoogleAuthDto {
  @IsNotEmpty({ message: 'Google ID Token is required' })
  @IsString()
  id_token: string;

  @IsOptional()
  @IsString()
  device_info?: string;
}
