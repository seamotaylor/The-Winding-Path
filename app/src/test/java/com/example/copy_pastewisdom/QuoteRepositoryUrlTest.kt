package com.example.copy_pastewisdom

import com.example.copy_pastewisdom.data.QuoteRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class QuoteRepositoryUrlTest {

    @Test
    fun `formatImageUrl should handle various Google Drive formats`() {
        val repo = QuoteRepository
        val header = "Author,About,Quote,Image\n"
        
        // Standard /d/ format
        assertEquals(
            "https://lh3.googleusercontent.com/d/1A2B3C4D5E6F",
            repo.parseCsv(header + "Author,Bio,Quote,https://drive.google.com/file/d/1A2B3C4D5E6F/view?usp=sharing", 3)[0].imageUrl
        )

        // open?id= format
        assertEquals(
            "https://lh3.googleusercontent.com/d/1A2B3C4D5E6F",
            repo.parseCsv(header + "Author,Bio,Quote,https://drive.google.com/open?id=1A2B3C4D5E6F", 3)[0].imageUrl
        )

        // uc?id= format
        assertEquals(
            "https://lh3.googleusercontent.com/d/1A2B3C4D5E6F",
            repo.parseCsv(header + "Author,Bio,Quote,https://docs.google.com/uc?id=1A2B3C4D5E6F&export=download", 3)[0].imageUrl
        )
    }

    @Test
    fun `normalizeUrl should preserve case`() {
        val repo = QuoteRepository
        val url = "https://example.com/Image_ABC_123.jpg"
        assertEquals(url, repo.normalizeUrl(url))
        
        // Should still strip trailing slash and http
        assertEquals("https://example.com/Path", repo.normalizeUrl("http://example.com/Path/"))
    }
}
