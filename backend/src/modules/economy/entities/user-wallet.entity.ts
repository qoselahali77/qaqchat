import {
  Entity,
  PrimaryColumn,
  Column,
  UpdateDateColumn,
  OneToOne,
  JoinColumn,
  Check,
  VersionColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

@Entity('user_wallets')
@Check(`"coins_balance" >= 0`)
@Check(`"earnings_balance" >= 0`)
export class UserWallet {
  @PrimaryColumn({ type: 'uuid' })
  user_id: string;

  @OneToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  // Coins: Purchased via Google Play / spent ONLY on gifts (non-withdrawable)
  @Column({ type: 'bigint', default: 0 })
  coins_balance: string; // Stored as string in JS to safely handle 64-bit bigints

  // Earnings/Gems: Accumulated from received gifts / eligible for withdrawal ONLY
  @Column({ type: 'bigint', default: 0 })
  earnings_balance: string;

  @VersionColumn({ default: 1 })
  version: number;

  @UpdateDateColumn({ type: 'timestamp with time zone' })
  updated_at: Date;
}
