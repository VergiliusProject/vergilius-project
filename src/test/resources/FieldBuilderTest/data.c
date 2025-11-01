// Definitions of basic Windows types
typedef void VOID;
typedef char CHAR;
typedef unsigned char UCHAR;
typedef unsigned short WCHAR;
typedef short SHORT;
typedef unsigned short USHORT;
typedef long LONG;
typedef unsigned long ULONG;
typedef long long LONGLONG;
typedef unsigned long long ULONGLONG;

// Test data structures
#include "data/test01"
struct test01 test01;

#include "data/issue27"
struct issue27 issue27;

// Main function to avoid linker errors
int main() {
    return 0;
}