import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  Check,
} from 'typeorm';

@Entity('gifts')
@Check(`"coin_price" > 0`)
@Check(`"diamond_reward" > 0`)
export class Gift {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ type: 'varchar', length: 64 })
  name: string;

  @Column({ type: 'text' })
  icon_url: string;

  @Column({ type: 'text', nullable: true })
  animation_url: string | null;

  @Column({ type: 'bigint' })
  coin_price: string;

  @Column({ type: 'bigint' })
  diamond_reward: string;

  @Column({ type: 'boolean', default: true })
  is_active: boolean;

  @Column({ type: 'int', default: 0 })
  position: number;

  @CreateDateColumn({ type: 'timestamp with time zone' })
  created_at: Date;
}
