import { MigrationInterface, QueryRunner } from 'typeorm';

export class InitialAuthAndUsers1700000000000 implements MigrationInterface {
  name = 'InitialAuthAndUsers1700000000000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    // 1. Enable UUID Extension if not enabled
    await queryRunner.query(`CREATE EXTENSION IF NOT EXISTS "uuid-ossp";`);

    // 2. Create users table
    await queryRunner.query(`
      CREATE TABLE "users" (
        "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
        "username" character varying(32) NOT NULL,
        "email" character varying(255) NOT NULL,
        "password_hash" character varying(255) NOT NULL,
        "display_name" character varying(64) NOT NULL,
        "avatar_url" text,
        "status" character varying(20) NOT NULL DEFAULT 'offline',
        "bio" character varying(250),
        "is_banned" boolean NOT NULL DEFAULT false,
        "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        CONSTRAINT "UQ_users_username" UNIQUE ("username"),
        CONSTRAINT "UQ_users_email" UNIQUE ("email"),
        CONSTRAINT "PK_users_id" PRIMARY KEY ("id")
      );
    `);

    // 3. Create auth_sessions table
    await queryRunner.query(`
      CREATE TABLE "auth_sessions" (
        "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
        "user_id" uuid NOT NULL,
        "refresh_token_hash" character varying(255) NOT NULL,
        "device_info" character varying(255),
        "ip_address" character varying(45),
        "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL,
        "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        CONSTRAINT "PK_auth_sessions_id" PRIMARY KEY ("id"),
        CONSTRAINT "FK_auth_sessions_user_id" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE NO ACTION
      );
    `);
    await queryRunner.query(`CREATE INDEX "IDX_auth_sessions_user_id" ON "auth_sessions" ("user_id");`);

    // 4. Create user_blocks table
    await queryRunner.query(`
      CREATE TABLE "user_blocks" (
        "blocker_id" uuid NOT NULL,
        "blocked_id" uuid NOT NULL,
        "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        CONSTRAINT "PK_user_blocks" PRIMARY KEY ("blocker_id", "blocked_id"),
        CONSTRAINT "FK_user_blocks_blocker" FOREIGN KEY ("blocker_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE NO ACTION,
        CONSTRAINT "FK_user_blocks_blocked" FOREIGN KEY ("blocked_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE NO ACTION
      );
    `);

    // 5. Create user_reports table
    await queryRunner.query(`
      CREATE TABLE "user_reports" (
        "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
        "reporter_id" uuid NOT NULL,
        "reported_user_id" uuid NOT NULL,
        "reason" character varying(100) NOT NULL,
        "description" text,
        "status" character varying(20) NOT NULL DEFAULT 'pending',
        "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        CONSTRAINT "PK_user_reports_id" PRIMARY KEY ("id"),
        CONSTRAINT "FK_user_reports_reporter" FOREIGN KEY ("reporter_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE NO ACTION,
        CONSTRAINT "FK_user_reports_reported" FOREIGN KEY ("reported_user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE NO ACTION
      );
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "user_reports";`);
    await queryRunner.query(`DROP TABLE IF EXISTS "user_blocks";`);
    await queryRunner.query(`DROP INDEX IF EXISTS "IDX_auth_sessions_user_id";`);
    await queryRunner.query(`DROP TABLE IF EXISTS "auth_sessions";`);
    await queryRunner.query(`DROP TABLE IF EXISTS "users";`);
  }
}
