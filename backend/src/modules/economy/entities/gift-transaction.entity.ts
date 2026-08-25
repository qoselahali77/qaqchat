import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Gift } from './gift.entity';

@Entity('gift_transactions')
export class GiftTransaction {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'uuid' })
  sender_id: string;

  @Column({ type: 'uuid' })
  receiver_id: string;

  @ManyToOne(() => User)
  @JoinColumn({ name: 'sender_id' })
  sender: User;

  @ManyToOne(() => User)
  @JoinColumn({ name: 'receiver_id' })
  receiver: User;

  @Column({ type: 'uuid', nullable: true })
  room_id: string | null;

  @Column({ type: 'uuid' })
  gift_id: string;

  @ManyToOne(() => Gift)
  @JoinColumn({ name: 'gift_id' })
  gift: Gift;

  @Column({ type: 'bigint' })
  coin_price: string;

  @Column({ type: 'bigint' })
  diamond_reward: string;

  @CreateDateColumn({ type: 'timestamp with time zone' })
  created_at: Date;
}
