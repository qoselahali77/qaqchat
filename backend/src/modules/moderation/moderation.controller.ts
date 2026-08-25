import {
  Controller,
  Post,
  Delete,
  Get,
  Body,
  Param,
  UseGuards,
} from '@nestjs/common';
import { ModerationService } from './moderation.service';
import { CreateReportDto, BlockUserDto } from './dto/moderation.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';

@Controller({ path: 'moderation', version: '1' })
@UseGuards(JwtAuthGuard)
export class ModerationController {
  constructor(private readonly moderationService: ModerationService) {}

  @Post('reports')
  async createReport(
    @CurrentUser('id') userId: string,
    @Body() dto: CreateReportDto,
  ) {
    return this.moderationService.reportUser(userId, dto);
  }

  @Post('blocks')
  async blockUser(
    @CurrentUser('id') userId: string,
    @Body() dto: BlockUserDto,
  ) {
    return this.moderationService.blockUser(userId, dto);
  }

  @Delete('blocks/:userId')
  async unblockUser(
    @CurrentUser('id') userId: string,
    @Param('userId') targetUserId: string,
  ) {
    return this.moderationService.unblockUser(userId, targetUserId);
  }

  @Get('blocks')
  async getBlockedUsers(@CurrentUser('id') userId: string) {
    return this.moderationService.getBlockedUsers(userId);
  }
}
