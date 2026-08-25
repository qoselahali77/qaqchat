import {
  Entity,
  PrimaryColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
} from 'typeorm';
import { User } from './user.entity';

@Entity('user_blocks')
export class UserBlock {
  @PrimaryColumn({ type: 'uuid' })
  blocker_id: string;

  @PrimaryColumn({ type: 'uuid' })
  blocked_id: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'blocker_id' })
  blocker: User;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'blocked_id' })
  blocked: User;

  @CreateDateColumn({ type: 'timestamp with time zone' })
  created_at: Date;
}
