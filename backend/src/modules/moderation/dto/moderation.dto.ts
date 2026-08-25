import { IsNotEmpty, IsOptional, IsString, IsUUID, Length } from 'class-validator';

export class CreateReportDto {
  @IsNotEmpty()
  @IsUUID()
  reported_user_id: string;

  @IsNotEmpty()
  @IsString()
  @Length(3, 100)
  reason: string;

  @IsOptional()
  @IsString()
  @Length(0, 1000)
  description?: string;
}

export class BlockUserDto {
  @IsNotEmpty()
  @IsUUID()
  user_id_to_block: string;
}
