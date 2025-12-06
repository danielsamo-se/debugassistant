import { test, expect } from '@playwright/test';

test('analyze stacktrace and show results', async ({ page }) => {
  await page.route('**/api/analyze', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        language: 'java',
        exceptionType: 'MethodArgumentNotValidException',
        rootCause: 'email must be a well-formed email address',
        keywords: ['spring', 'validation', 'email'],
        results: [
          {
            source: 'stackoverflow',
            title: 'Spring validation email error',
            url: 'https://stackoverflow.com/q/123',
            score: 0.9,
            reactions: 42,
            answerCount: 3,
            isAnswered: true,
          },
        ],
      }),
    });
  });

  await page.goto('/');

  await page.fill(
    'textarea',
    `org.springframework.web.bind.MethodArgumentNotValidException:
Validation failed for argument [0]`,
  );

  await page.click('button:has-text("Analyze")');

  await expect(page.locator('text=Found 1 Solutions')).toBeVisible({
    timeout: 10_000,
  });

  await expect(
    page.locator('strong', {
      hasText: 'MethodArgumentNotValidException',
    }),
  ).toBeVisible();
  await expect(page.locator('a[target="_blank"]')).toBeVisible();
});
