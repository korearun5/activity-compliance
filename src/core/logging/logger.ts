type LogContext = Record<string, unknown>;

function isDevelopment() {
  return typeof __DEV__ === "undefined" ? true : __DEV__;
}

function writeLog(
  level: "debug" | "error" | "info" | "warn",
  message: string,
  context?: LogContext
) {
  if (level === "debug" && !isDevelopment()) {
    return;
  }

  const payload = context ? [message, context] : [message];

  if (level === "error") {
    console.error(...payload);
    return;
  }

  if (level === "warn") {
    console.warn(...payload);
    return;
  }

  console.log(...payload);
}

export const logger = {
  debug: (message: string, context?: LogContext) =>
    writeLog("debug", message, context),
  error: (message: string, context?: LogContext) =>
    writeLog("error", message, context),
  info: (message: string, context?: LogContext) =>
    writeLog("info", message, context),
  warn: (message: string, context?: LogContext) =>
    writeLog("warn", message, context)
};

