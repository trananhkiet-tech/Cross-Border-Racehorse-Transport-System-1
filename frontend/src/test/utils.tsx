import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { render } from '@testing-library/react';
import type { UserSummary } from '../types/api';
import type { Role } from '../types/enums';

export function makeUser(role: Role): UserSummary {
  return { id: 1, email: `${role.toLowerCase()}@irtms.vn`, fullName: 'Người dùng thử', role, enabled: true };
}

export function renderAt(ui: ReactElement, path = '/') {
  return render(<MemoryRouter initialEntries={[path]}>{ui}</MemoryRouter>);
}
