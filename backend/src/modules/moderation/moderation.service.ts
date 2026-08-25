import {
  Injectable,
  BadRequestException,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { UserReport, ReportStatus } from '../users/entities/user-report.entity';
import { UserBlock } from '../users/entities/user-block.entity';
import { User } from '../users/entities/user.entity';
import { CreateReportDto, BlockUserDto } from './dto/moderation.dto';

@Injectable()
export class ModerationService {
  constructor(
    @InjectRepository(UserReport)
    private readonly reportRepository: Repository<UserReport>,
    @InjectRepository(UserBlock)
    private readonly blockRepository: Repository<UserBlock>,
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
  ) {}

  async reportUser(reporterId: string, dto: CreateReportDto): Promise<UserReport> {
    if (reporterId === dto.reported_user_id) {
      throw new BadRequestException('You cannot report yourself');
    }

    const targetUser = await this.userRepository.findOne({
      where: { id: dto.reported_user_id },
    });
    if (!targetUser) {
      throw new NotFoundException('Reported user not found');
    }

    const report = this.reportRepository.create({
      reporter_id: reporterId,
      reported_user_id: dto.reported_user_id,
      reason: dto.reason,
      description: dto.description || null,
      status: ReportStatus.PENDING,
    });

    return this.reportRepository.save(report);
  }

  async blockUser(blockerId: string, dto: BlockUserDto): Promise<{ success: boolean; message: string }> {
    if (blockerId === dto.user_id_to_block) {
      throw new BadRequestException('You cannot block yourself');
    }

    const targetUser = await this.userRepository.findOne({
      where: { id: dto.user_id_to_block },
    });
    if (!targetUser) {
      throw new NotFoundException('User to block not found');
    }

    const existingBlock = await this.blockRepository.findOne({
      where: { blocker_id: blockerId, blocked_id: dto.user_id_to_block },
    });

    if (existingBlock) {
      return { success: true, message: 'User is already blocked' };
    }

    const block = this.blockRepository.create({
      blocker_id: blockerId,
      blocked_id: dto.user_id_to_block,
    });

    await this.blockRepository.save(block);
    return { success: true, message: 'User blocked successfully' };
  }

  async unblockUser(blockerId: string, targetUserId: string): Promise<{ success: boolean; message: string }> {
    await this.blockRepository.delete({
      blocker_id: blockerId,
      blocked_id: targetUserId,
    });
    return { success: true, message: 'User unblocked successfully' };
  }

  async getBlockedUsers(userId: string): Promise<User[]> {
    const blocks = await this.blockRepository.find({
      where: { blocker_id: userId },
      relations: ['blocked'],
    });
    return blocks.map((b) => b.blocked);
  }
}
