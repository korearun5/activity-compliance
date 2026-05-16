type SessionExpiredEvent = {
  message: string;
  path: string;
  status: number;
};

type SessionExpiredListener = (event: SessionExpiredEvent) => void | Promise<void>;

const sessionExpiredListeners = new Set<SessionExpiredListener>();
let sessionExpiredNotified = false;

export function subscribeSessionExpired(listener: SessionExpiredListener) {
  sessionExpiredListeners.add(listener);

  return () => {
    sessionExpiredListeners.delete(listener);
  };
}

export function notifySessionExpired(event: SessionExpiredEvent) {
  if (sessionExpiredNotified) {
    return;
  }

  sessionExpiredNotified = true;
  sessionExpiredListeners.forEach((listener) => {
    void listener(event);
  });
}

export function resetSessionExpiredNotification() {
  sessionExpiredNotified = false;
}
