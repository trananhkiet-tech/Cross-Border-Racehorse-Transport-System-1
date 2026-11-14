import type { StaffRole } from '../types/enums';

/** Vai trò trên chuyến phù hợp với chuyên môn trong hồ sơ nhân sự: tài xế chỉ làm tài xế; áp tải / trưởng nhóm không lái. */
export function rolesFor(staffRole?: StaffRole): StaffRole[] {
  return staffRole === 'DRIVER' ? ['DRIVER'] : staffRole === 'ESCORT' || staffRole === 'TEAM_LEADER' ? ['ESCORT', 'TEAM_LEADER'] : ['DRIVER', 'ESCORT', 'TEAM_LEADER'];
}
