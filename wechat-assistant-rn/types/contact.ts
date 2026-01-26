export interface Contact {
  id: string;
  name: string;
  avatarUrl?: string;
  phoneNumber?: string;
  lastCallTime?: Date;
}

export enum CallState {
  Idle = 'idle',
  Connecting = 'connecting',
  Ringing = 'ringing',
  Connected = 'connected',
  Ended = 'ended',
}

export interface CallSession {
  contact: Contact;
  state: CallState;
  startTime?: Date;
  duration?: number;
}
