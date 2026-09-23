package android.net

import android.os.Parcel

class FakeUri : Uri() {
    override fun toString(): String = "content://fake/1"
    override fun compareTo(other: Uri?): Int = 0
    override fun isHierarchical() = true
    override fun isRelative() = false
    override fun getScheme() = "content"
    override fun getEncodedSchemeSpecificPart() = ""
    override fun getSchemeSpecificPart() = ""
    override fun getAuthority() = "fake"
    override fun getEncodedAuthority() = "fake"
    override fun getUserInfo() = null
    override fun getEncodedUserInfo() = null
    override fun getHost() = "fake"
    override fun getPort() = -1
    override fun getPath() = "/1"
    override fun getEncodedPath() = "/1"
    override fun getQuery() = null
    override fun getEncodedQuery() = null
    override fun getFragment() = null
    override fun getEncodedFragment() = null
    override fun getPathSegments() = emptyList<String>()
    override fun getLastPathSegment() = "1"
    override fun buildUpon() = null
    override fun describeContents() = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {}
}
