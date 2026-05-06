# syntax=docker/dockerfile:1

FROM node:22-alpine

WORKDIR /app
ENV EXPO_NO_TELEMETRY=1

COPY package.json package-lock.json ./
RUN npm ci

COPY app.json babel.config.js eslint.config.mjs tsconfig.json ./
COPY App.tsx ./
COPY src src

EXPOSE 19006 8081

CMD ["npm", "run", "web", "--", "--host", "0.0.0.0", "--port", "19006"]
