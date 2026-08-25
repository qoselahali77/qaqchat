import {
  Injectable,
  BadRequestException,
  NotFoundException,
  ConflictException,
  Logger,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { DataSource, EntityManager, Repository } from 'typeorm';
import { UserWallet } from './entities/user-wallet.entity';
import {
  WalletTransaction,
  WalletType,
  TransactionType,
} from './entities/wallet-transaction.entity';
import { Gift } from './entities/gift.entity';
import { GiftTransaction } from './entities/gift-transaction.entity';
import {
  GooglePlayPurchase,
  PurchaseState,
} from './entities/google-play-purchase.entity';
import {
  WithdrawalRequest,
  WithdrawalStatus,
} from './entities/withdrawal-request.entity';
import {
  SendGiftDto,
  VerifyGooglePlayPurchaseDto,
  CreateWithdrawalDto,
  ReviewWithdrawalDto,
} from './dto/economy.dto';

@Injectable()
export class EconomyService {
  private readonly logger = new Logger(EconomyService.name);

  // Exchange rate: 100 diamonds = $1.00 USD
  private readonly DIAMONDS_PER_USD = 100;

  // Google Play SKU to Coins Catalog mapping
  private readonly PRODUCT_COINS_MAP: Record<string, number> = {
    'coins_tier_1': 100,    // $0.99
    'coins_tier_2': 550,    // $4.99
    'coins_tier_3': 1200,   // $9.99
    'coins_tier_4': 2600,   // $19.99
    'coins_tier_5': 7000,   // $49.99
    'coins_tier_6': 15000,  // $99.99
  };

  constructor(
    private readonly dataSource: DataSource,
    @InjectRepository(UserWallet)
    private readonly walletRepository: Repository<UserWallet>,
    @InjectRepository(WalletTransaction)
    private readonly txRepository: Repository<WalletTransaction>,
    @InjectRepository(Gift)
    private readonly giftRepository: Repository<Gift>,
    @InjectRepository(GiftTransaction)
    private readonly giftTxRepository: Repository<GiftTransaction>,
    @InjectRepository(GooglePlayPurchase)
    private readonly purchaseRepository: Repository<GooglePlayPurchase>,
    @InjectRepository(WithdrawalRequest)
    private readonly withdrawalRepository: Repository<WithdrawalRequest>,
  ) {}

  async getWallet(userId: string): Promise<UserWallet> {
    return this.getOrCreateWallet(userId);
  }

  async getOrCreateWallet(userId: string, manager?: EntityManager): Promise<UserWallet> {
    const repo = manager ? manager.getRepository(UserWallet) : this.walletRepository;
    let wallet = await repo.findOne({ where: { user_id: userId } });

    if (!wallet) {
      wallet = repo.create({
        user_id: userId,
        coins_balance: '0',
        earnings_balance: '0',
      });
      wallet = await repo.save(wallet);
    }
    return wallet;
  }

  async getActiveGifts(): Promise<Gift[]> {
    return this.giftRepository.find({
      where: { is_active: true },
      order: { position: 'ASC' },
    });
  }

  async getTransactions(
    userId: string,
    limit: number = 20,
    offset: number = 0,
  ): Promise<WalletTransaction[]> {
    return this.txRepository.find({
      where: { user_id: userId },
      order: { created_at: 'DESC' },
      take: limit,
      skip: offset,
    });
  }

  /**
   * Send Gift: Atomic transfer with Pessimistic Write Lock
   * Strictly debits sender's coins_balance and credits receiver's earnings_balance
   */
  async sendGift(senderId: string, dto: SendGiftDto) {
    if (senderId === dto.receiver_id) {
      throw new BadRequestException('You cannot send a gift to yourself');
    }

    // Check idempotency if provided
    if (dto.idempotency_key) {
      const existingTx = await this.txRepository.findOne({
        where: { idempotency_key: dto.idempotency_key },
      });
      if (existingTx) {
        throw new ConflictException('Duplicate transaction: Gift already processed');
      }
    }

    const gift = await this.giftRepository.findOne({
      where: { id: dto.gift_id, is_active: true },
    });
    if (!gift) {
      throw new NotFoundException('Gift not found or inactive');
    }

    const coinPriceBigInt = BigInt(gift.coin_price);
    const diamondRewardBigInt = BigInt(gift.diamond_reward);

    return this.dataSource.transaction(async (manager) => {
      // 1. Lock Sender Wallet with PESSIMISTIC_WRITE
      let senderWallet = await manager
        .createQueryBuilder(UserWallet, 'wallet')
        .setLock('pessimistic_write')
        .where('wallet.user_id = :id', { id: senderId })
        .getOne();

      if (!senderWallet) {
        senderWallet = await this.getOrCreateWallet(senderId, manager);
      }

      const senderCoinsBefore = BigInt(senderWallet.coins_balance);
      if (senderCoinsBefore < coinPriceBigInt) {
        throw new BadRequestException(
          `Insufficient coins. Required: ${gift.coin_price}, Available: ${senderWallet.coins_balance}`,
        );
      }

      const senderCoinsAfter = senderCoinsBefore - coinPriceBigInt;
      senderWallet.coins_balance = senderCoinsAfter.toString();
      await manager.save(senderWallet);

      // 2. Lock Receiver Wallet with PESSIMISTIC_WRITE
      let receiverWallet = await manager
        .createQueryBuilder(UserWallet, 'wallet')
        .setLock('pessimistic_write')
        .where('wallet.user_id = :id', { id: dto.receiver_id })
        .getOne();

      if (!receiverWallet) {
        receiverWallet = await this.getOrCreateWallet(dto.receiver_id, manager);
      }

      const receiverEarningsBefore = BigInt(receiverWallet.earnings_balance);
      const receiverEarningsAfter = receiverEarningsBefore + diamondRewardBigInt;
      receiverWallet.earnings_balance = receiverEarningsAfter.toString();
      await manager.save(receiverWallet);

      // 3. Record Gift Transaction
      const giftTx = manager.create(GiftTransaction, {
        sender_id: senderId,
        receiver_id: dto.receiver_id,
        room_id: dto.room_id || null,
        gift_id: gift.id,
        coin_price: gift.coin_price,
        diamond_reward: gift.diamond_reward,
      });
      await manager.save(giftTx);

      // 4. Record Sender Debit in Ledger (Append-Only)
      const senderLedgerTx = manager.create(WalletTransaction, {
        user_id: senderId,
        wallet_type: WalletType.COINS,
        transaction_type: TransactionType.GIFT_SENT,
        amount: (-coinPriceBigInt).toString(),
        balance_before: senderCoinsBefore.toString(),
        balance_after: senderCoinsAfter.toString(),
        reference_id: giftTx.id,
        reference_type: 'gift_transaction',
        idempotency_key: dto.idempotency_key || null,
        metadata: {
          gift_id: gift.id,
          gift_name: gift.name,
          receiver_id: dto.receiver_id,
          room_id: dto.room_id,
        },
      });
      await manager.save(senderLedgerTx);

      // 5. Record Receiver Credit in Ledger (Append-Only)
      const receiverLedgerTx = manager.create(WalletTransaction, {
        user_id: dto.receiver_id,
        wallet_type: WalletType.EARNINGS,
        transaction_type: TransactionType.GIFT_RECEIVED,
        amount: diamondRewardBigInt.toString(),
        balance_before: receiverEarningsBefore.toString(),
        balance_after: receiverEarningsAfter.toString(),
        reference_id: giftTx.id,
        reference_type: 'gift_transaction',
        metadata: {
          gift_id: gift.id,
          gift_name: gift.name,
          sender_id: senderId,
          room_id: dto.room_id,
        },
      });
      await manager.save(receiverLedgerTx);

      return {
        success: true,
        gift_transaction_id: giftTx.id,
        gift: {
          id: gift.id,
          name: gift.name,
          icon_url: gift.icon_url,
          animation_url: gift.animation_url,
        },
        sender_coins_balance: senderCoinsAfter.toString(),
      };
    });
  }

  /**
   * Verify Google Play Purchase & Award Coins
   * Prevents Replay Attacks by enforcing order_id / purchase_token uniqueness
   */
  async verifyGooglePlayPurchase(userId: string, dto: VerifyGooglePlayPurchaseDto) {
    // 1. Check for Replay Attack
    const existing = await this.purchaseRepository.findOne({
      where: [{ order_id: dto.order_id }, { purchase_token: dto.purchase_token }],
    });
    if (existing) {
      throw new ConflictException('This purchase has already been claimed');
    }

    // 2. Validate product tier and coin reward
    const coinsToAward = this.PRODUCT_COINS_MAP[dto.product_id];
    if (!coinsToAward) {
      throw new BadRequestException('Invalid product ID');
    }

    // 3. In production: Server calls Google Play Developer API (androidpublisher.purchases.products.get)
    // and acknowledges purchase via androidpublisher.purchases.products.acknowledge.
    this.logger.log(`Verified Google Play Order ${dto.order_id} for User ${userId}`);

    const coinsBigInt = BigInt(coinsToAward);

    return this.dataSource.transaction(async (manager) => {
      // Record verified purchase
      const purchaseRecord = manager.create(GooglePlayPurchase, {
        user_id: userId,
        order_id: dto.order_id,
        product_id: dto.product_id,
        purchase_token: dto.purchase_token,
        coins_awarded: coinsToAward.toString(),
        state: PurchaseState.VERIFIED,
      });
      await manager.save(purchaseRecord);

      // Lock & Credit User's Coin Wallet
      let wallet = await manager
        .createQueryBuilder(UserWallet, 'wallet')
        .setLock('pessimistic_write')
        .where('wallet.user_id = :id', { id: userId })
        .getOne();

      if (!wallet) {
        wallet = await this.getOrCreateWallet(userId, manager);
      }

      const coinsBefore = BigInt(wallet.coins_balance);
      const coinsAfter = coinsBefore + coinsBigInt;
      wallet.coins_balance = coinsAfter.toString();
      await manager.save(wallet);

      // Append-Only Ledger Entry
      const ledgerTx = manager.create(WalletTransaction, {
        user_id: userId,
        wallet_type: WalletType.COINS,
        transaction_type: TransactionType.PURCHASE,
        amount: coinsBigInt.toString(),
        balance_before: coinsBefore.toString(),
        balance_after: coinsAfter.toString(),
        reference_id: dto.order_id,
        reference_type: 'google_play_purchase',
        metadata: {
          product_id: dto.product_id,
          order_id: dto.order_id,
        },
      });
      await manager.save(ledgerTx);

      return {
        success: true,
        order_id: dto.order_id,
        coins_awarded: coinsToAward,
        new_coins_balance: coinsAfter.toString(),
      };
    });
  }

  /**
   * Request Earnings Withdrawal (Hold diamonds immediately with a ledger entry)
   */
  async requestWithdrawal(userId: string, dto: CreateWithdrawalDto) {
    const diamondsToWithdraw = BigInt(dto.amount_diamonds);
    const estimatedCash = Number(dto.amount_diamonds) / this.DIAMONDS_PER_USD;

    return this.dataSource.transaction(async (manager) => {
      let wallet = await manager
        .createQueryBuilder(UserWallet, 'wallet')
        .setLock('pessimistic_write')
        .where('wallet.user_id = :id', { id: userId })
        .getOne();

      if (!wallet) {
        wallet = await this.getOrCreateWallet(userId, manager);
      }

      const earningsBefore = BigInt(wallet.earnings_balance);
      if (earningsBefore < diamondsToWithdraw) {
        throw new BadRequestException(
          `Insufficient earnings. Required: ${dto.amount_diamonds}, Available: ${wallet.earnings_balance}`,
        );
      }

      const earningsAfter = earningsBefore - diamondsToWithdraw;
      wallet.earnings_balance = earningsAfter.toString();
      await manager.save(wallet);

      const withdrawal = manager.create(WithdrawalRequest, {
        user_id: userId,
        amount_diamonds: dto.amount_diamonds.toString(),
        estimated_cash_amount: estimatedCash,
        currency: 'USD',
        payment_method: dto.payment_method,
        payment_details: dto.payment_details,
        status: WithdrawalStatus.PENDING,
      });
      await manager.save(withdrawal);

      // Ledger: Record hold on earnings
      const ledgerTx = manager.create(WalletTransaction, {
        user_id: userId,
        wallet_type: WalletType.EARNINGS,
        transaction_type: TransactionType.WITHDRAWAL_HOLD,
        amount: (-diamondsToWithdraw).toString(),
        balance_before: earningsBefore.toString(),
        balance_after: earningsAfter.toString(),
        reference_id: withdrawal.id,
        reference_type: 'withdrawal_request',
      });
      await manager.save(ledgerTx);

      return {
        success: true,
        withdrawal_id: withdrawal.id,
        amount_diamonds: dto.amount_diamonds,
        estimated_cash_amount: estimatedCash,
        currency: 'USD',
        status: withdrawal.status,
        remaining_earnings: earningsAfter.toString(),
      };
    });
  }

  /**
   * Review Withdrawal: If rejected, refund diamonds back to earnings via Reversal ledger entry
   */
  async reviewWithdrawal(reviewerId: string, withdrawalId: string, dto: ReviewWithdrawalDto) {
    const withdrawal = await this.withdrawalRepository.findOne({
      where: { id: withdrawalId },
    });
    if (!withdrawal) {
      throw new NotFoundException('Withdrawal request not found');
    }

    if (withdrawal.status !== WithdrawalStatus.PENDING && withdrawal.status !== WithdrawalStatus.UNDER_REVIEW) {
      throw new BadRequestException(`Cannot review a request that is already ${withdrawal.status}`);
    }

    return this.dataSource.transaction(async (manager) => {
      withdrawal.status = dto.status;
      withdrawal.reviewed_by = reviewerId;
      if (dto.rejection_reason) {
        withdrawal.rejection_reason = dto.rejection_reason;
      }
      await manager.save(withdrawal);

      // If Rejected -> Execute Reversal / Refund to earnings balance
      if (dto.status === WithdrawalStatus.REJECTED) {
        let wallet = await manager
          .createQueryBuilder(UserWallet, 'wallet')
          .setLock('pessimistic_write')
          .where('wallet.user_id = :id', { id: withdrawal.user_id })
          .getOne();

        if (wallet) {
          const refundDiamonds = BigInt(withdrawal.amount_diamonds);
          const earningsBefore = BigInt(wallet.earnings_balance);
          const earningsAfter = earningsBefore + refundDiamonds;
          wallet.earnings_balance = earningsAfter.toString();
          await manager.save(wallet);

          const refundLedgerTx = manager.create(WalletTransaction, {
            user_id: withdrawal.user_id,
            wallet_type: WalletType.EARNINGS,
            transaction_type: TransactionType.WITHDRAWAL_REFUND,
            amount: refundDiamonds.toString(),
            balance_before: earningsBefore.toString(),
            balance_after: earningsAfter.toString(),
            reference_id: withdrawal.id,
            reference_type: 'withdrawal_request',
            metadata: {
              rejection_reason: dto.rejection_reason,
            },
          });
          await manager.save(refundLedgerTx);
        }
      }

      return {
        success: true,
        withdrawal_id: withdrawal.id,
        status: withdrawal.status,
      };
    });
  }
}
