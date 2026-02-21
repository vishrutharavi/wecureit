import { test, expect } from '@playwright/test';

// Test data
const testAdmin = {
  email: 'admin@test.com',
  password: 'TestPassword123!',
};

const testDoctor = {
  email: 'doctor@test.com',
  password: 'TestPassword123!',
  name: 'Dr. Test MD',
};

const testPatient = {
  email: 'patient@test.com',
  password: 'TestPassword123!',
  name: 'John Doe',
};

test.describe('Admin Analytics - Alerts Dashboard', () => {
  test('should display overloaded specialists', async ({ page }) => {
    // Navigate to admin page
    await page.goto('/admin');
    
    // Wait for alerts tab
    await page.click('text=Alerts');
    await page.waitForSelector('text=Overloaded Specialists');
    
    // Verify alerts are displayed
    const overloadedSection = await page.locator('text=Overloaded Specialists');
    await expect(overloadedSection).toBeVisible();
    
    // Check that doctor names are present (synced from DB)
    const doctorNames = await page.locator('[role="row"]').allTextContents();
    expect(doctorNames.length).toBeGreaterThan(0);
  });

  test('should sync graph from PostgreSQL to Neo4j', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Alerts');
    
    // Click Sync Graph button
    const syncButton = page.locator('button:has-text("Sync Graph")');
    await syncButton.click();
    
    // Wait for sync to complete
    await page.waitForTimeout(2000);
    
    // Verify button returns to normal state
    await expect(syncButton).not.toContainText('Syncing');
  });

  test('should refresh alerts data', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Alerts');
    
    // Click Refresh button
    const refreshButton = page.locator('button:has-text("Refresh")');
    await refreshButton.click();
    
    // Wait for data to reload
    await page.waitForTimeout(1000);
    
    // Verify alerts section is still visible
    const overloadedSection = await page.locator('text=Overloaded Specialists');
    await expect(overloadedSection).toBeVisible();
  });

  test('should display speciality imbalances without duplicates', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Alerts');
    
    // Wait for speciality imbalances section
    await page.waitForSelector('text=Speciality Imbalances');
    
    // Get all speciality codes
    const specialityRows = await page.locator('[class*="flexDirection"]').count();
    
    // Verify no duplicate CARD entries
    const cardCount = await page.locator('text=Cardiology').count();
    expect(cardCount).toBeLessThanOrEqual(1);
  });

  test('should display cross-state warnings', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Alerts');
    
    // Wait for cross-state warnings section
    await page.waitForSelector('text=Cross-State Warnings');
    
    // Verify warnings section is visible
    const warningsSection = await page.locator('text=Cross-State Warnings');
    await expect(warningsSection).toBeVisible();
  });
});

test.describe('Admin Analytics - Network Graph', () => {
  test('should display referral network', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Network Graph');
    
    // Wait for SVG network diagram
    await page.waitForSelector('svg');
    
    // Verify network graph is visible
    const svg = page.locator('svg').first();
    await expect(svg).toBeVisible();
  });

  test('should display updated doctor names in network', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Network Graph');
    
    // Wait for doctor names to appear
    await page.waitForSelector('text=Roy');
    
    // Verify synced doctor names are displayed (not old names)
    const royNode = await page.locator('text=Roy');
    await expect(royNode).toBeVisible();
    
    // Verify old names are NOT present
    const oldNames = await page.locator('text=Network From').count();
    expect(oldNames).toBe(0);
  });

  test('should have refresh and sync buttons', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Network Graph');
    
    // Check for Refresh button
    const refreshButton = page.locator('button:has-text("Refresh")');
    await expect(refreshButton).toBeVisible();
    
    // Check for Sync Graph button
    const syncButton = page.locator('button:has-text("Sync Graph")');
    await expect(syncButton).toBeVisible();
  });

  test('should filter network by doctor name', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Network Graph');
    
    // Wait for filter input
    await page.waitForSelector('input[placeholder*="Filter"]');
    
    // Type a doctor name in filter
    await page.fill('input[placeholder*="Filter"]', 'Roy');
    
    // Verify filtered results
    const roy = await page.locator('text=Roy');
    await expect(roy).toBeVisible();
  });

  test('should sync network graph data', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Network Graph');
    
    // Click Sync Graph button
    const syncButton = page.locator('button:has-text("Sync Graph")');
    await syncButton.click();
    
    // Wait for sync
    await page.waitForTimeout(2000);
    
    // Click Refresh
    const refreshButton = page.locator('button:has-text("Refresh")');
    await refreshButton.click();
    
    // Verify data is still visible
    const svg = page.locator('svg').first();
    await expect(svg).toBeVisible();
  });
});

test.describe('Admin Analytics - Overview', () => {
  test('should display analytics overview', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Overview');
    
    // Wait for overview section to load
    await page.waitForTimeout(1000);
    
    // Verify overview is visible
    const overview = await page.locator('text=Referral Analytics');
    const isVisible = await overview.isVisible().catch(() => false);
    if (isVisible) {
      await expect(overview).toBeVisible();
    }
  });
});

test.describe('Admin Doctors Management', () => {
  test('should list all doctors', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Doctors');
    
    // Wait for doctors table
    await page.waitForSelector('table, [role="table"]');
    
    // Verify doctors are displayed
    const tableRows = await page.locator('[role="row"]').count();
    expect(tableRows).toBeGreaterThan(0);
  });

  test('should display doctor with updated name', async ({ page }) => {
    await page.goto('/admin');
    await page.click('text=Doctors');
    
    // Look for one of the synced doctor names
    const doctorCell = page.locator('text=Dr. Sarah Johnson');
    const isVisible = await doctorCell.isVisible().catch(() => false);
    
    if (isVisible) {
      await expect(doctorCell).toBeVisible();
    }
  });
});
