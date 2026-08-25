import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
  Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

export enum WalletType {
  COINS = 'coins',
  EARNINGS = 'earnings',
}

export enum TransactionType {
  PURCHASE = 'purchase',
  GIFT_SENT = 'gift_sent',
  GIFT_RECEIVED = 'gift_received',
  WITHDRAWAL_HOLD = 'withdrawal_hold',
  WITHDRAWAL_REFUND = 'withdrawal_refund',
  WITHDRAWAL_COMPLETE = 'withdrawal_complete',
  ADMIN_ADJUSTMENT = 'admin_adjustment',
  REVERSAL = 'reversal',
}

@Entity('wallet_transactions')
@Index(['user_id', 'created_at'])
export class WalletTransaction {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  user_id: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Column({ type: 'varchar', length: 20 })
  wallet_type: WalletType;

  @Column({ type: 'varchar', length: 30 })
  transaction_type: TransactionType;

  // Positive for credit/deposit, negative for debit/expenditure
  @Column({ type: 'bigint' })
  amount: string;

  @Column({ type: 'bigint' })
  balance_before: string;

  @Column({ type: 'bigint' })
  balance_after: string;

  @Column({ type: 'varchar', length: 100, nullable: true })
  reference_id: string | null;

  @Column({ type: 'varchar', length: 50, nullable: true })
  reference_type: string | null;

  @Index({ unique: true })
  @Column({ type: 'varchar', length: 100, unique: true, nullable: true })
  idempotency_key: string | null;

  @Column({ type: 'jsonb', nullable: true })
  metadata: Record<string, any> | null;

  @CreateDateColumn({ type: 'timestamp with time zone' })
  created_at: Date;
}
