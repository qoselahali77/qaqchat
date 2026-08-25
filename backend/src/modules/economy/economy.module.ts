import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UserWallet } from './entities/user-wallet.entity';
import { WalletTransaction } from './entities/wallet-transaction.entity';
import { Gift } from './entities/gift.entity';
import { GiftTransaction } from './entities/gift-transaction.entity';
import { GooglePlayPurchase } from './entities/google-play-purchase.entity';
import { WithdrawalRequest } from './entities/withdrawal-request.entity';
import { EconomyService } from './economy.service';
import { EconomyController } from './economy.controller';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      UserWallet,
      WalletTransaction,
      Gift,
      GiftTransaction,
      GooglePlayPurchase,
      WithdrawalRequest,
    ]),
  ],
  providers: [EconomyService],
  controllers: [EconomyController],
  exports: [EconomyService],
})
export class EconomyModule {}
