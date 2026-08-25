import { NestFactory } from '@nestjs/core';
import { ValidationPipe, VersioningType } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { AllExceptionsFilter } from './common/filters/all-exceptions.filter';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // 1. Enable Global Prefix
  app.setGlobalPrefix('api');

  // 2. Enable API Versioning (/api/v1/...)
  app.enableVersioning({
    type: VersioningType.URI,
    defaultVersion: '1',
  });

  // 3. Enable Global Validation Pipe
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  // 4. Global Exception Filter
  app.useGlobalFilters(new AllExceptionsFilter());

  // 5. CORS configuration
  app.enableCors({
    origin: true,
    credentials: true,
  });

  // 6. Swagger API Documentation
  const config = new DocumentBuilder()
    .setTitle('Chat & Voice Professional API')
    .setDescription('Versioned REST API for ChatQAQ Backend (Zero-Downtime Architecture)')
    .setVersion('1.0')
    .addBearerAuth()
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api/docs', app, document);

  const port = process.env.PORT || 3000;
  await app.listen(port);
  console.log(`🚀 Chat & Voice API running on: http://localhost:${port}/api/v1`);
  console.log(`📑 Swagger Documentation available on: http://localhost:${port}/api/docs`);
}
bootstrap();
