import crypto from 'node:crypto'

export const FOREVER = -1;

type Entry<T> = {
  value: T,
  when: number
}

export function randId(): string {
  return crypto.randomBytes(32).toString('base64url');
}

class TimedMap<T> {
  private readonly entries = new Map<String, Entry<T>>();

  set(k: string, v: T, w: number): void {
    this.entries.set(k, { value: v, when: w });
  }

  get(k: string): T | null {
    const e = this.entries.get(k);

    if (!e)
      return null;


    if (e.when != FOREVER && e.when <= Date.now()) {
      this.entries.delete(k);
      return null;
    }

    return e.value;
  }

  take(k: string): T | null {
    const v = this.get(k);
    if (v)
      this.entries.delete(k);

    return v;
  }
}

export type User = {
  id: string
  email: string
  family_name: string
  given_name: string
}

export const states = new TimedMap<string>();

export const sessions = new TimedMap<User>();

export const tickets = new TimedMap<string>();

