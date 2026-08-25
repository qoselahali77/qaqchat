import { MigrationInterface, QueryRunner } from 'typeorm';

export class AddGoogleAuthAndNameFields1700000002000 implements MigrationInterface {
  name = 'AddGoogleAuthAndNameFields1700000002000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "first_name" character varying(64);`);
    await queryRunner.query(`ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "last_name" character varying(64);`);
    await queryRunner.query(`ALTER TABLE "users" ADD COLUMN IF NOT EXISTS "google_id" character varying(100);`);
    await queryRunner.query(`ALTER TABLE "users" ALTER COLUMN "password_hash" DROP NOT NULL;`);
    await queryRunner.query(`CREATE UNIQUE INDEX IF NOT EXISTS "UQ_users_google_id" ON "users" ("google_id") WHERE "google_id" IS NOT NULL;`);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP INDEX IF EXISTS "UQ_users_google_id";`);
    await queryRunner.query(`ALTER TABLE "users" DROP COLUMN IF EXISTS "google_id";`);
    await queryRunner.query(`ALTER TABLE "users" DROP COLUMN IF EXISTS "last_name";`);
    await queryRunner.query(`ALTER TABLE "users" DROP COLUMN IF EXISTS "first_name";`);
    await queryRunner.query(`ALTER TABLE "users" ALTER COLUMN "password_hash" SET NOT NULL;`);
  }
}
