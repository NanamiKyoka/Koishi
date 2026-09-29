package com.nanami.koishi.feature.tools.postal_code.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostalDatasetTest {

    @Test
    fun `parses raw json and keeps valid six digit postal codes`() {
        val dataset = PostalDataset.fromRaw(RAW_JSON)

        assertEquals(6, dataset.size)
        assertTrue(dataset.findByPostalCode("100010").any { it.district == "东城区" })
    }

    @Test
    fun `drops malformed postal codes from raw json`() {
        val dataset = PostalDataset.fromRaw(DIRTY_JSON)

        assertEquals(1, dataset.size)
        assertEquals("凤泉区", dataset.findByPostalCode("453001").single().district)
    }

    @Test
    fun `returns every district sharing a postal code`() {
        val dataset = PostalDataset.fromRaw(RAW_JSON)

        val shared = dataset.findByPostalCode("518000")
        assertEquals(3, shared.size)
        assertEquals(
            setOf("福田区", "南山区", "盐田区"),
            shared.map { it.district }.toSet()
        )
    }

    @Test
    fun `search matches district name city province and pinyin`() {
        val dataset = PostalDataset.fromRaw(RAW_JSON)

        assertEquals("东城区", dataset.search("东城").single().district)
        assertEquals("东城区", dataset.search("dongcheng").single().district)
        assertEquals("东城区", dataset.search("东城区").single().district)
        assertTrue(dataset.search("朝阳区").size >= 1)
    }

    @Test
    fun `search ranks exact district match ahead of partial matches`() {
        val dataset = PostalDataset.fromRaw("""
            [
              {"province":"北京市","city":"北京市","name":"朝阳区","zipCode":"100020","pinyin":"chaoyang"},
              {"province":"吉林省","city":"长春市","name":"朝阳区","zipCode":"130012","pinyin":"chaoyang"},
              {"province":"北京市","city":"北京市","name":"朝阳区扩展区","zipCode":"100021","pinyin":"chaoyangkuoqu"}
            ]
        """.trimIndent())

        val results = dataset.search("朝阳区")

        assertEquals("100020", results.first().postalCode)
        assertEquals(3, results.size)
    }

    @Test
    fun `search ignores blank query`() {
        val dataset = PostalDataset.fromRaw(RAW_JSON)

        assertTrue(dataset.search("   ").isEmpty())
    }

    @Test
    fun `compact round trip preserves every record`() {
        val original = PostalDataset.fromRaw(RAW_JSON)

        val restored = PostalDataset.fromCompact(PostalDataset.toCompact(original))

        assertEquals(original.size, restored.size)
        assertEquals(
            original.findByPostalCode("518000").map { it.district }.toSet(),
            restored.findByPostalCode("518000").map { it.district }.toSet()
        )
        assertEquals("云南省", restored.search("五华").single().province)
    }

    @Test
    fun `malformed compact payload yields empty dataset`() {
        assertEquals(0, PostalDataset.fromCompact("garbage").size)
        assertEquals(0, PostalDataset.fromCompact("").size)
    }

    private companion object {
        val RAW_JSON = """
            [
              {"code":"110101","name":"东城区","cityCode":"1101","provinceCode":"11","province":"北京市","city":"北京市","zipCode":"100010","pinyin":"dong cheng"},
              {"code":"110105","name":"朝阳区","cityCode":"1101","provinceCode":"11","province":"北京市","city":"北京市","zipCode":"100020","pinyin":"chao yang"},
              {"code":"440304","name":"福田区","cityCode":"4403","provinceCode":"44","province":"广东省","city":"深圳市","zipCode":"518000","pinyin":"fu tian"},
              {"code":"440305","name":"南山区","cityCode":"4403","provinceCode":"44","province":"广东省","city":"深圳市","zipCode":"518000","pinyin":"nan shan"},
              {"code":"440306","name":"盐田区","cityCode":"4403","provinceCode":"44","province":"广东省","city":"深圳市","zipCode":"518000","pinyin":"yan tian"},
              {"code":"530102","name":"五华区","cityCode":"5301","provinceCode":"53","province":"云南省","city":"昆明市","zipCode":"650100","pinyin":"wu hua"}
            ]
        """.trimIndent()

        val DIRTY_JSON = """
            [
              {"province":"河南省","city":"新乡市","name":"凤泉区","zipCode":"4453011","pinyin":"feng quan"},
              {"province":"福建省","city":"泉州市","name":"金门县","zipCode":"890至896","pinyin":"jin men"},
              {"province":"北京市","city":"北京市","name":"国外","zipCode":"","pinyin":"guo wai"},
              {"province":"河南省","city":"新乡市","name":"凤泉区","zipCode":"453001","pinyin":"feng quan"}
            ]
        """.trimIndent()
    }
}
