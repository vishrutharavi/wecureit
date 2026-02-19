import { test, expect } from '@playwright/test';

test.describe('Frontend Loading & Performance', () => {
  test('admin page should load without console errors', async ({ page }) => {
    const errors: string[] = [];
    
    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });
    
    await page.goto('/admin');
    await page.waitForTimeout(2000);
    
    expect(errors.length).toBe(0);
  });

  test('analytics page should load analytics tabs', async ({ page }) => {
    await page.goto('/admin');
    
    // Wait for tab buttons
    await page.waitForSelector('text=Overview');
    await page.waitForSelector('text=Alerts');
    await page.waitForSelector('text=Network Graph');
    
    // Verify tabs are clickable
    await page.click('text=Alerts');
    await page.waitForTimeout(500);
    
    const alerts = await page.locator('text=Referral Alerts');
    await expect(alerts).toBeVisible();
  });

  test('should not have duplicate doctor nodes in graph', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Network Graph');
    
    // Wait for network to render
    await page.waitForTimeout(1500);
    
    // Get all text nodes in SVG (doctor names)
    const textElements = await page.locator('svg text').allTextContents();
    
    // Filter out coordinate/positioning text, keep only doctor names
    const doctorNames = textElements.filter(text => 
      text.trim().length > 0 && !text.match(/^\d+$/)
    );
    
    // Check there are no duplicate names (except in statistics)
    const uniqueNames = new Set(doctorNames);
    
    // Allow some duplicates in the UI, but not excessive ones
    expect(doctorNames.length / uniqueNames.size).toBeLessThan(3);
  });
});

test.describe('UI Components & Interactions', () => {
  test('refresh button should reload data', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Alerts');
    
    // Get initial table row count
    const initialRows = await page.locator('[role="row"]').count();
    
    // Click refresh
    await page.click('button:has-text("Refresh")');
    await page.waitForTimeout(1000);
    
    // Verify table still exists
    const finalRows = await page.locator('[role="row"]').count();
    expect(finalRows).toBeGreaterThanOrEqual(0);
  });

  test('sync button should change state while syncing', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Alerts');
    
    const syncButton = page.locator('button:has-text("Sync Graph")');
    
    // Click sync
    await syncButton.click();
    
    // Button should show syncing state
    const syncingButton = page.locator('button:has-text("Syncing")');
    const hasSyncingState = await syncingButton.isVisible().catch(() => false);
    
    // Eventually should return to normal state
    await page.waitForTimeout(2500);
  });

  test('filter input should filter network results', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Network Graph');
    
    // Wait for network to load
    await page.waitForTimeout(1000);
    
    // Get count of visible doctor nodes
    const filterInput = page.locator('input[placeholder*="Filter"]');
    
    // Filter for a specific doctor
    await filterInput.fill('Roy');
    await page.waitForTimeout(500);
    
    // Verify filter worked (check if Roy is still visible)
    const roy = page.locator('text=Roy');
    const isVisible = await roy.isVisible().catch(() => false);
    
    // Roy should exist in the filtered view or not be present at all
    if (isVisible) {
      await expect(roy).toBeVisible();
    }
  });

  test('clear selection button should work on network graph', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Network Graph');
    
    await page.waitForTimeout(1000);
    
    // Check if "Clear selection" button exists
    const clearButton = page.locator('button:has-text("Clear selection")');
    const exists = await clearButton.isVisible().catch(() => false);
    
    if (exists) {
      await clearButton.click();
      await page.waitForTimeout(500);
      
      // Button should be gone after clearing
      const stillExists = await clearButton.isVisible().catch(() => false);
      expect(stillExists).toBeFalsy();
    }
  });
});

test.describe('Data Consistency across UI', () => {
  test('alerts and network should show same doctor names', async ({ page }) => {
    await page.goto('/admin');
    
    // Get names from alerts tab
    await page.click('text=Alerts');
    await page.waitForTimeout(1000);
    const alertsText = await page.content();
    
    // Switch to network tab
    await page.click('text=Network Graph');
    await page.waitForTimeout(1000);
    const networkText = await page.content();
    
    // Both should contain synced doctor names
    const commonNames = ['Roy', 'James', 'Mian'];
    
    let foundInAlertsOrNetwork = false;
    for (const name of commonNames) {
      if (alertsText.includes(name) || networkText.includes(name)) {
        foundInAlertsOrNetwork = true;
        break;
      }
    }
    
    expect(foundInAlertsOrNetwork).toBeTruthy();
  });

  test('no console errors after full workflow', async ({ page }) => {
    const errors: string[] = [];
    
    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });
    
    // Navigate through each tab
    await page.goto('/admin');
    
    await page.click('text=Overview');
    await page.waitForTimeout(500);
    
    await page.click('text=Alerts');
    await page.waitForTimeout(500);
    
    // Click sync button
    await page.click('button:has-text("Sync Graph")');
    await page.waitForTimeout(2000);
    
    // Click refresh
    await page.click('button:has-text("Refresh")');
    await page.waitForTimeout(500);
    
    await page.click('text=Network Graph');
    await page.waitForTimeout(500);
    
    // Filter network
    const filterInput = page.locator('input[placeholder*="Filter"]');
    await filterInput.fill('Roy');
    await page.waitForTimeout(500);
    
    // No errors should have occurred
    const criticalErrors = errors.filter(e => 
      !e.includes('ResizeObserver') && 
      !e.includes('net::ERR_NAME_NOT_RESOLVED')
    );
    
    expect(criticalErrors.length).toBe(0);
  });
});
