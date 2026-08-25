import {
  IsEnum,
  IsNotEmpty,
  IsNumber,
  IsObject,
  IsOptional,
  IsPositive,
  IsString,
  IsUUID,
  Min,
} from 'class-validator';
import { WithdrawalStatus } from '../entities/withdrawal-request.entity';

export class SendGiftDto {
  @IsNotEmpty()
  @IsUUID()
  receiver_id: string;

  @IsNotEmpty()
  @IsUUID()
  gift_id: string;

  @IsOptional()
  @IsUUID()
  room_id?: string;

  @IsOptional()
  @IsString()
  idempotency_key?: string;
}

export class VerifyGooglePlayPurchaseDto {
  @IsNotEmpty()
  @IsString()
  order_id: string;

  @IsNotEmpty()
  @IsString()
  product_id: string;

  @IsNotEmpty()
  @IsString()
  purchase_token: string;
}

export class CreateWithdrawalDto {
  @IsNotEmpty()
  @IsNumber()
  @IsPositive()
  @Min(1000, { message: 'Minimum withdrawal amount is 1000 diamonds' })
  amount_diamonds: number;

  @IsNotEmpty()
  @IsString()
  payment_method: string;

  @IsNotEmpty()
  @IsObject()
  payment_details: Record<string, any>;
}

export class ReviewWithdrawalDto {
  @IsNotEmpty()
  @IsEnum([WithdrawalStatus.APPROVED, WithdrawalStatus.REJECTED, WithdrawalStatus.COMPLETED])
  status: WithdrawalStatus;

  @IsOptional()
  @IsString()
  rejection_reason?: string;
}
