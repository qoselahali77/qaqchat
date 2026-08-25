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

export enum PurchaseState {
  VERIFIED = 'verified',
  CONSUMED = 'consumed',
  REFUNDED = 'refunded',
}

@Entity('google_play_purchases')
export class GooglePlayPurchase {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  user_id: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Index({ unique: true })
  @Column({ type: 'varchar', length: 100, unique: true })
  order_id: string;

  @Column({ type: 'varchar', length: 100 })
  product_id: string;

  @Index({ unique: true })
  @Column({ type: 'varchar', length: 255, unique: true })
  purchase_token: string;

  @Column({ type: 'bigint' })
  coins_awarded: string;

  @Column({
    type: 'varchar',
    length: 30,
    default: PurchaseState.VERIFIED,
  })
  state: PurchaseState;

  @Column({ type: 'timestamp with time zone', default: () => 'CURRENT_TIMESTAMP' })
  verified_at: Date;

  @CreateDateColumn({ type: 'timestamp with time zone' })
  created_at: Date;
}
