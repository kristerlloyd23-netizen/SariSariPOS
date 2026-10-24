package com.sarisari.pos.domain

import com.sarisari.pos.data.local.entity.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakBulkTest {

    private val parent = ProductEntity(
        id = 1, name = "Noodle carton", category = Categories.OTHERS,
        costPrice = 100.0, sellingPrice = 120.0, stockQuantity = 3
    )
    private val child = ProductEntity(
        id = 2, name = "Noodle sachet", category = Categories.OTHERS,
        costPrice = 5.0, sellingPrice = 7.0, stockQuantity = 4,
        isBreakBulk = true, parentProductId = 1, conversionRate = 24
    )

    @Test fun `loose stock is used before opening a carton`() {
        val result = BreakBulk.planDeduction(child, parent, 3)
        assertNotNull(result)
        assertEquals(1, result!!.newChildStock)
        assertEquals(3, result.newParentStock)
        assertEquals(0, result.parentUnitsOpened)
    }

    @Test fun `shortfall automatically opens enough parent units`() {
        val result = BreakBulk.planDeduction(child, parent, 25)
        assertNotNull(result)
        assertEquals(3, result!!.newChildStock)
        assertEquals(2, result.newParentStock)
        assertEquals(1, result.parentUnitsOpened)
    }

    @Test fun `insufficient loose and bulk stock is rejected`() {
        val emptyParent = parent.copy(stockQuantity = 0)
        assertNull(BreakBulk.planDeduction(child, emptyParent, 5))
    }

    @Test fun `cart availability includes parent units but accounts direct parent sales`() {
        val products = mapOf(parent.id to parent, child.id to child)
        assertTrue(BreakBulk.isCartFeasible(mapOf(child.id to 52), products))
        assertFalse(BreakBulk.isCartFeasible(mapOf(child.id to 77), products))
        assertFalse(BreakBulk.isCartFeasible(mapOf(parent.id to 3, child.id to 5), products))
    }

    @Test fun `regular items do not borrow stock from a parent`() {
        val plain = child.copy(isBreakBulk = false, parentProductId = null)
        assertNull(BreakBulk.planDeduction(plain, parent, 5))
        assertEquals(0, BreakBulk.unitsToOpen(plain, 10))
    }
}
