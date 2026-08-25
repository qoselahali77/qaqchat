import { DataSource, DataSourceOptions } from 'typeorm';
import * as dotenv from 'dotenv';
dotenv.config();

const isProduction = process.env.NODE_ENV === 'production';
const databaseUrl = process.env.DATABASE_URL;

export const dataSourceOptions: DataSourceOptions = databaseUrl
  ? {
      type: 'postgres',
      url: databaseUrl,
      ssl: {
        rejectUnauthorized: false,
      },
      entities: [__dirname + '/../**/*.entity.{js,ts}'],
      migrations: [__dirname + '/migrations/*.{js,ts}'],
      migrationsRun: true, // Automatically run pending migrations on startup in cloud environments
      synchronize: false,
      logging: !isProduction,
    }
  : {
      type: 'postgres',
      host: process.env.DB_HOST || 'localhost',
      port: parseInt(process.env.DB_PORT, 10) || 5432,
      username: process.env.DB_USERNAME || 'postgres',
      password: process.env.DB_PASSWORD || 'postgres',
      database: process.env.DB_DATABASE || 'chat_voice_db',
      entities: [__dirname + '/../**/*.entity.{js,ts}'],
      migrations: [__dirname + '/migrations/*.{js,ts}'],
      synchronize: false,
      logging: !isProduction,
    };

const dataSource = new DataSource(dataSourceOptions);
export default dataSource;
