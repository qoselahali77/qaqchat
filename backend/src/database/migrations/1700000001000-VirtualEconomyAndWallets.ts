import { MigrationInterface, QueryRunner } from 'typeorm';

export class VirtualEconomyAndWallets1700000001000 implements MigrationInterface {
  name = 'VirtualEconomyAndWallets1700000001000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    // 1. Create user_wallets table
    await queryRunner.query(`
      CREATE TABLE "user_wallets" (
        "user_id" uuid NOT NULL,
        "coins_balance" bigint NOT NULL DEFAULT 0,
        "earnings_balance" bigint NOT NULL DEFAULT 0,
        "version" integer NOT NULL DEFAULT 1,
        "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        CONSTRAINT "PK_user_wallets" PRIMARY KEY ("user_id"),
        CONSTRAINT "CHK_user_wallets_coins" CHECK ("coins_balance" >= 0),
        CONSTRAINT "CHK_user_wallets_earnings" CHECK ("earnings_balance" >= 0),
        CONSTRAINT "FK_user_wallets_user_id" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE NO ACTION
      );
    `);

    // 2. Create wallet_transactions table (Immutable Ledger)
    await queryRunner.query(`
      CREATE TABLE "wallet_transactions" (
        "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
        "user_id" uuid NOT NULL,
        "wallet_type" character varying(20) NOT NULL,
        "transaction_type" character varying(30) NOT NULL,
        "amount" bigint NOT NULL,
        "balance_before" bigint NOT NULL,
        "balance_after" bigint NOT NULL,
        "reference_id" character varying(100),
        "reference_type" character varying(50),
        "idempotency_key" character varying(100),
        "metadata" jsonb,
        "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        CONSTRAINT "PK_wallet_transactions_id" PRIMARY KEY ("id"),
        CONSTRAINT "UQ_wallet_transactions_idempotency" UNIQUE ("idempotency_key"),
        CONSTRAINT "FK_wallet_transactions_user_id" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE NO ACTION
      );
    `);
    await queryRunner.query(`CREATE INDEX "IDX_wallet_tx_user_created" ON "wallet_transactions" ("user_id", "created_at" DESC);`);

    // 3. Create gifts table
    await queryRunner.query(`
      CREATE TABLE "gifts" (
        "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
        "name" character varying(64) NOT NULL,
        "icon_url" text NOT NULL,
        "animation_url" text,
        "coin_price" bigint NOT NULL,
        "diamond_reward" bigint NOT NULL,
        "is_active" boolean NOT NULL DEFAULT true,
        "position" integer NOT NULL DEFAULT 0,
        "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        CONSTRAINT "PK_gifts_id" PRIMARY KEY ("id"),
        CONSTRAINT "CHK_gifts_coin_price" CHECK ("coin_price" > 0),
        CONSTRAINT "CHK_gifts_diamond_reward" CHECK ("diamond_reward" > 0)
      );
    `);

    // 4. Create gift_transactions table
    await queryRunner.query(`
      CREATE TABLE "gift_transactions" (
        "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
        "sender_id" uuid NOT NULL,
        "receiver_id" uuid NOT NULL,
        "room_id" uuid,
        "gift_id" uuid NOT NULL,
        "coin_price" bigint NOT NULL,
        "diamond_reward" bigint NOT NULL,
        "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        CONSTRAINT "PK_gift_transactions_id" PRIMARY KEY ("id"),
        CONSTRAINT "FK_gift_tx_sender" FOREIGN KEY ("sender_id") REFERENCES "users"("id") ON DELETE NO ACTION ON UPDATE NO ACTION,
        CONSTRAINT "FK_gift_tx_receiver" FOREIGN KEY ("receiver_id") REFERENCES "users"("id") ON DELETE NO ACTION ON UPDATE NO ACTION,
        CONSTRAINT "FK_gift_tx_gift" FOREIGN KEY ("gift_id") REFERENCES "gifts"("id") ON DELETE NO ACTION ON UPDATE NO ACTION
      );
    `);

    // 5. Create google_play_purchases table
    await queryRunner.query(`
      CREATE TABLE "google_play_purchases" (
        "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
        "user_id" uuid NOT NULL,
        "order_id" character varying(100) NOT NULL,
        "product_id" character varying(100) NOT NULL,
        "purchase_token" character varying(255) NOT NULL,
        "coins_awarded" bigint NOT NULL,
        "state" character varying(30) NOT NULL DEFAULT 'verified',
        "verified_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        CONSTRAINT "PK_google_play_purchases_id" PRIMARY KEY ("id"),
        CONSTRAINT "UQ_google_play_purchases_order_id" UNIQUE ("order_id"),
        CONSTRAINT "UQ_google_play_purchases_token" UNIQUE ("purchase_token"),
        CONSTRAINT "FK_google_play_purchases_user" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE NO ACTION
      );
    `);

    // 6. Create withdrawal_requests table
    await queryRunner.query(`
      CREATE TABLE "withdrawal_requests" (
        "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
        "user_id" uuid NOT NULL,
        "amount_diamonds" bigint NOT NULL,
        "estimated_cash_amount" numeric(10,2) NOT NULL,
        "currency" character varying(3) NOT NULL DEFAULT 'USD',
        "payment_method" character varying(50) NOT NULL,
        "payment_details" jsonb NOT NULL,
        "status" character varying(30) NOT NULL DEFAULT 'pending',
        "rejection_reason" text,
        "reviewed_by" uuid,
        "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        CONSTRAINT "PK_withdrawal_requests_id" PRIMARY KEY ("id"),
        CONSTRAINT "CHK_withdrawal_requests_diamonds" CHECK ("amount_diamonds" > 0),
        CONSTRAINT "FK_withdrawal_requests_user" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE NO ACTION,
        CONSTRAINT "FK_withdrawal_requests_reviewer" FOREIGN KEY ("reviewed_by") REFERENCES "users"("id") ON DELETE SET NULL ON UPDATE NO ACTION
      );
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "withdrawal_requests";`);
    await queryRunner.query(`DROP TABLE IF EXISTS "google_play_purchases";`);
    await queryRunner.query(`DROP TABLE IF EXISTS "gift_transactions";`);
    await queryRunner.query(`DROP TABLE IF EXISTS "gifts";`);
    await queryRunner.query(`DROP INDEX IF EXISTS "IDX_wallet_tx_user_created";`);
    await queryRunner.query(`DROP TABLE IF EXISTS "wallet_transactions";`);
    await queryRunner.query(`DROP TABLE IF EXISTS "user_wallets";`);
  }
}
