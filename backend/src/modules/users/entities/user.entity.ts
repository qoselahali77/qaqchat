import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  Index,
} from 'typeorm';

export enum UserStatus {
  ONLINE = 'online',
  OFFLINE = 'offline',
  AWAY = 'away',
  DND = 'dnd',
}

@Entity('users')
export class User {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Index({ unique: true })
  @Column({ type: 'varchar', length: 32, unique: true })
  username: string;

  @Index({ unique: true })
  @Column({ type: 'varchar', length: 255, unique: true })
  email: string;

  @Column({ type: 'varchar', length: 255, select: false, nullable: true })
  password_hash: string | null;

  @Column({ type: 'varchar', length: 64, nullable: true })
  first_name: string | null;

  @Column({ type: 'varchar', length: 64, nullable: true })
  last_name: string | null;

  @Column({ type: 'varchar', length: 64 })
  display_name: string;

  @Index({ unique: true })
  @Column({ type: 'varchar', length: 100, unique: true, nullable: true })
  google_id: string | null;

  @Column({ type: 'text', nullable: true })
  avatar_url: string | null;

  @Column({
    type: 'varchar',
    length: 20,
    default: UserStatus.OFFLINE,
  })
  status: UserStatus;

  @Column({ type: 'varchar', length: 250, nullable: true })
  bio: string | null;

  @Column({ type: 'boolean', default: false })
  is_banned: boolean;

  @CreateDateColumn({ type: 'timestamp with time zone' })
  created_at: Date;

  @UpdateDateColumn({ type: 'timestamp with time zone' })
  updated_at: Date;
}
