import {
  Controller,
  Get,
  Post,
  Patch,
  Body,
  Param,
  Query,
  UseGuards,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { EconomyService } from './economy.service';
import {
  SendGiftDto,
  VerifyGooglePlayPurchaseDto,
  CreateWithdrawalDto,
  ReviewWithdrawalDto,
} from './dto/economy.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';

@Controller({ path: 'economy', version: '1' })
@UseGuards(JwtAuthGuard)
export class EconomyController {
  constructor(private readonly economyService: EconomyService) {}

  @Get('wallet')
  async getWallet(@CurrentUser('id') userId: string) {
    return this.economyService.getWallet(userId);
  }

  @Get('wallet/transactions')
  async getTransactions(
    @CurrentUser('id') userId: string,
    @Query('limit') limit: number = 20,
    @Query('offset') offset: number = 0,
  ) {
    return this.economyService.getTransactions(userId, limit, offset);
  }

  @Get('gifts')
  async getGifts() {
    return this.economyService.getActiveGifts();
  }

  @HttpCode(HttpStatus.OK)
  @Post('gifts/send')
  async sendGift(
    @CurrentUser('id') userId: string,
    @Body() dto: SendGiftDto,
  ) {
    return this.economyService.sendGift(userId, dto);
  }

  @HttpCode(HttpStatus.OK)
  @Post('billing/google-play/verify')
  async verifyPurchase(
    @CurrentUser('id') userId: string,
    @Body() dto: VerifyGooglePlayPurchaseDto,
  ) {
    return this.economyService.verifyGooglePlayPurchase(userId, dto);
  }

  @Post('withdrawals')
  async requestWithdrawal(
    @CurrentUser('id') userId: string,
    @Body() dto: CreateWithdrawalDto,
  ) {
    return this.economyService.requestWithdrawal(userId, dto);
  }

  @Patch('withdrawals/:id/review')
  async reviewWithdrawal(
    @CurrentUser('id') reviewerId: string,
    @Param('id') withdrawalId: string,
    @Body() dto: ReviewWithdrawalDto,
  ) {
    return this.economyService.reviewWithdrawal(reviewerId, withdrawalId, dto);
  }
}
