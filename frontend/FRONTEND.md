# FRONTEND.md — стайлгайд фронтенда risk-scoring

Обязателен к соблюдению во всех фронт-задачах этого проекта по умолчанию, без
отдельного напоминания. Отклонение — только по явному указанию пользователя.

---

## Стек

React + Vite + TypeScript · Tailwind CSS v4 · Framer Motion (`motion`) ·
`react-router` (роутинг) · `@stomp/stompjs` (STOMP-клиент живого стрима скана) ·
`@fontsource/space-grotesk` + `@fontsource/jetbrains-mono` (шрифты через npm,
не Google Fonts CDN).

Граф связей, когда появятся данные для него — готовая либа
(`react-force-graph`/`cytoscape.js`), с нуля не пилить.

## Структура

```
src/
  pages/            страницы, собираются из components/*
  components/ui/    переиспользуемые примитивы (Button, Card, Input, Select, RiskBadge, Spinner)
  components/       составные блоки, сгруппированные по фиче (hero/, console/, report/, layout/)
  lib/               api-клиент, ws-клиент, типы (зеркало DTO gateway), форматирование, справочники
  hooks/             переиспользуемая логика (useScanStream, useCountUp)
```

## Дизайн-токены — единственное место: `src/index.css`

```css
@theme {
  --color-bg: #0a0b0f; --color-surface: #12141b; --color-surface-2: #1a1d26; --color-border: #262a36;
  --color-text: #e6e8ee; --color-text-dim: #8a90a2; --color-text-faint: #4c5163;
  --color-accent: #00e5c7; --color-accent-press: #00b9a1;
  --color-risk-low: #35d07f; --color-risk-mid: #f5c451; --color-risk-high: #ff8c42; --color-risk-critical: #ff4d4d;
  --radius-base: 6px;
  --font-sans: "Space Grotesk", system-ui, sans-serif;
  --font-mono: "JetBrains Mono", ui-monospace, monospace;
}
```

Цвета/радиусы/шрифты нигде больше не хардкодятся. Единственный радиус —
`rounded-base`, без `rounded-lg/xl/full` вразнобой. Риск-цвета — единый источник
`lib/risk.ts` (`RISK` маппинг + `riskAccentClass()`), потребляется `RiskBadge` и
`VerdictReveal`, нигде не дублируются.

## Анти-слоп

Запрещено: дешёвые градиенты (особенно фиолетово-розовый), glassmorphism ради
glassmorphism, случайные blur/glow не по делу; шрифты Orbitron/Press Start;
эмодзи в UI; декоративные иконки без смысла; стоковые hero-секции; пружинящие
игривые анимации; разные радиусы/отступы «на глаз»; инлайн-стили; arbitrary-
значения Tailwind (`p-[13px]`, `text-[#abc]`) — только токены и штатная шкала.
Единственное согласованное исключение — фокус-свечение `HeroInput`
(`shadow-[...var(--color-accent)...]`): оно ссылается только на токены, не на
магические числа/цвета, и это часть первого «вау»-момента, эталон ниже.

## Арт-дирекшн

Тёмная тема, «forensic console / on-chain мониторинг». Не светлый лендинг с
градиентом. Моноширинный шрифт — адреса/хеши/суммы; обычный — текст. Один
неоновый акцент (`--color-accent`), не радуга. Движение точное и сдержанное —
инструмент безопасности, не игрушка.

## Бюджет анимации: три «вау», остальное — статично

1. **Hero-инпут на лендинге** — `HeroInput.tsx`: свечение на фокусе, плавный
   fade/scale-переход в консоль при сабмите.
2. **Живая консоль скана** — `ScanConsolePage.tsx` + `ConsoleLog`/`ConsoleLine`:
   строки WS-стрима появляются одна за одной. Анимация отражает реальные
   Kafka-события, не декорация.
3. **Ревил вердикта** — `VerdictReveal.tsx` + `ScoreCounter.tsx` (докрутка через
   `useCountUp`) + `EvidenceList.tsx` (staggered reveal доказательств).

Всё остальное (Dashboard/Watchlist/Alerts/Settings — пока `ComingSoonPage`) —
без спецэффектов. Анимации — только через Framer Motion (`animate`,
`motion.div`, `AnimatePresence`), не CSS-кейфреймы руками. Анимировать
`transform`/`opacity`, не layout-свойства.

## Эталоны

**HeroInput** (`src/components/hero/HeroInput.tsx`):

```tsx
export function HeroInput({ value, onChange, onSubmit, disabled }: HeroInputProps) {
  return (
    <div className="flex w-full max-w-2xl items-center gap-3 rounded-base border border-border
                    bg-surface px-6 py-4 font-mono transition-[border-color,box-shadow]
                    focus-within:border-accent focus-within:shadow-[0_0_0_1px_var(--color-accent),0_0_28px_-6px_var(--color-accent)]">
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && onSubmit()}
        disabled={disabled}
        placeholder="0x… адрес кошелька"
        className="flex-1 bg-transparent text-lg text-text outline-none placeholder:text-text-faint disabled:text-text-faint"
      />
    </div>
  );
}
```

**Card** (`src/components/ui/Card.tsx`):

```tsx
export function Card({ title, className, children }: CardProps) {
  return (
    <section className={cn("rounded-base border border-border bg-surface p-6", className)}>
      {title && <h3 className="mb-4 font-sans text-xs uppercase tracking-wider text-text-dim">{title}</h3>}
      {children}
    </section>
  );
}
```

**RiskBadge** (`src/components/ui/RiskBadge.tsx`, цвета из `src/lib/risk.ts`) —
единственный источник риск-цветов:

```tsx
// lib/risk.ts
export const RISK: Record<RiskLevel, { label: string; text: string; border: string }> = {
  LOW: { label: "LOW", text: "text-risk-low", border: "border-risk-low" },
  MEDIUM: { label: "MEDIUM", text: "text-risk-mid", border: "border-risk-mid" },
  HIGH: { label: "HIGH", text: "text-risk-high", border: "border-risk-high" },
  CRITICAL: { label: "CRITICAL", text: "text-risk-critical", border: "border-risk-critical" },
};

// components/ui/RiskBadge.tsx
export function RiskBadge({ level }: RiskBadgeProps) {
  const risk = RISK[level];
  return (
    <span className={cn("inline-flex items-center rounded-base border px-2.5 py-1 font-mono text-xs", risk.text, risk.border)}>
      {risk.label}
    </span>
  );
}
```

## Каноничный код

Ноль дублирования (повторяющийся набор классов → компонент), ноль мусора
(мёртвые классы, закомментированный код, неиспользуемые пропсы), строгая
типизация без `any`, один компонент — одна ответственность, единый `cn()`-хелпер
(`src/lib/cn.ts`) для условных классов вместо ручной конкатенации строк.

## Контракты с бэкендом

`src/lib/types.ts` — зеркало DTO/событий `gateway`
(`ScanCreateRequest`/`ScanAcceptedResponse`/`ScanView`/`ScanReportView`/
`ScanProgressMessage`, enums `ScanStage`/`ScanSource`/`RiskLevel`). При изменении
контракта на бэке — править здесь в первую очередь, до вёрстки. `src/lib/api.ts`
и `src/lib/ws.ts` — единственные места, где фронт ходит в сеть.

## i18n — источник строк живёт в бэкенде, не на фронте

Все тексты интерфейса (лендинг, консоль, отчёт, навигация, коды ошибок) хранятся
как Spring `MessageSource` в `gateway`:
`gateway/src/main/resources/i18n/messages.properties` (английский, дефолт) и
`messages_ru.properties` (русский). Фронт **не хранит переводов** — только
идентификаторы ключей, для тайпчека на местах вызова:

- `src/lib/i18n/messageKeys.ts` — union-тип `MessageKey` (только имена ключей,
  без текста) + `Locale`.
- `src/lib/i18n/context.ts` — React-контекст и хук `useI18n()` → `{ locale, setLocale, t }`.
- `src/lib/i18n/I18nProvider.tsx` — провайдер: детектит локаль (localStorage →
  `navigator.language` → `en`), при смене локали дёргает
  `GET /api/i18n?lang=en|ru` и кладёт результат в контекст. Оборачивает `<App/>`
  в `main.tsx`.
- `components/layout/LocaleSwitch.tsx` — переключатель EN/RU, в `NavBar` и
  отдельно в углу `LandingPage` (у лендинга своего header нет).

Добавил новую строку в UI → добавь ключ в оба `.properties`-файла в `gateway`
**и** в `messageKeys.ts`, вызови через `t("namespace.key")`. Хардкода текста в
JSX/TSX нет нигде, включая плейсхолдеры и `aria-label`.

`lib/api.ts` шлёт заголовок `Accept-Language` с текущей локалью на каждый
запрос — поэтому и серверные сообщения об ошибках (валидация, `UnsupportedChainException`
и т.д.) приходят на выбранном языке без доп. логики на фронте.
