#include <stdio.h>
#include <stdint.h>
#include <string.h>

#define BLOCK_SIZE		1024
#define BLOCKS			2048
#define FAT_SIZE		(BLOCKS * 2)
#define FAT_BLOCKS		(FAT_SIZE / BLOCK_SIZE)
#define ROOT_BLOCK		FAT_BLOCKS
#define DIR_ENTRY_SIZE		32
#define DIR_ENTRIES		(BLOCK_SIZE / DIR_ENTRY_SIZE)

/* FAT data structure */
int16_t fat[BLOCKS];
/* data block */
int8_t data_block[BLOCK_SIZE];

/* directory entry */
struct dir_entry_s {
	int8_t filename[25];
	int8_t attributes;
	int16_t first_block;
	int32_t size;
};

/* reads a data block from disk */
void read_block(char *file, int32_t block, int8_t *record)
{
	FILE *f;

	f = fopen(file, "r+");
	fseek(f, block * BLOCK_SIZE, SEEK_SET);
	fread(record, 1, BLOCK_SIZE, f);
	fclose(f);
}

/* writes a data block to disk */
void write_block(char *file, int32_t block, int8_t *record)
{
	FILE *f;

	f = fopen(file, "r+");
	fseek(f, block * BLOCK_SIZE, SEEK_SET);
	fwrite(record, 1, BLOCK_SIZE, f);
	fclose(f);
}

/* reads the FAT from disk */
void read_fat(char *file, int16_t *fat)
{
	FILE *f;

	f = fopen(file, "r+");
	fseek(f, 0, SEEK_SET);
	fread(fat, 2, BLOCKS, f);
	fclose(f);
}

/* writes the FAT to disk */
void write_fat(char *file, int16_t *fat)
{
	FILE *f;

	f = fopen(file, "r+");
	fseek(f, 0, SEEK_SET);
	fwrite(fat, 2, BLOCKS, f);
	fclose(f);
}

/* reads a directory entry from a directory */
void read_dir_entry(int32_t block, int32_t entry, struct dir_entry_s *dir_entry)
{
	read_block("filesystem.dat", block, data_block);
	memcpy(dir_entry, &data_block[entry * sizeof(struct dir_entry_s)], sizeof(struct dir_entry_s));
}

/* writes a directory entry in a directory */
void write_dir_entry(int block, int entry, struct dir_entry_s *dir_entry)
{
	read_block("filesystem.dat", block, data_block);
	memcpy(&data_block[entry * sizeof(struct dir_entry_s)], dir_entry, sizeof(struct dir_entry_s));
	write_block("filesystem.dat", block, data_block);
}

int main(void)
{
	FILE *f;
	int32_t i;
	struct dir_entry_s dir_entry;

	/* create filesystem.dat if it doesn't exist */
	f = fopen("filesystem.dat", "a");
	fclose(f);

	/* initialize the FAT */
	for (i = 0; i < FAT_BLOCKS; i++)
		fat[i] = 0x7ffe;
	fat[ROOT_BLOCK] = 0x7fff;
	for (i = ROOT_BLOCK + 1; i < BLOCKS; i++)
		fat[i] = 0;
	/* write it to disk */
	write_fat("filesystem.dat", fat);

	/* initialize an empty data block */
	for (i = 0; i < BLOCK_SIZE; i++)
		data_block[i] = 0;

	/* write an empty ROOT directory block */
	write_block("filesystem.dat", ROOT_BLOCK, data_block);

	/* write the remaining data blocks to disk */
	for (i = ROOT_BLOCK + 1; i < BLOCKS; i++)
		write_block("filesystem.dat", i, data_block);

	/* fill three root directory entries and list them */
	memset((char *)dir_entry.filename, 0, sizeof(struct dir_entry_s));
	strcpy((char *)dir_entry.filename, "file1");
	dir_entry.attributes = 0x01;
	dir_entry.first_block = 1111;
	dir_entry.size = 222;
	write_dir_entry(ROOT_BLOCK, 0, &dir_entry);

	memset((char *)dir_entry.filename, 0, sizeof(struct dir_entry_s));
	strcpy((char *)dir_entry.filename, "file2");
	dir_entry.attributes = 0x01;
	dir_entry.first_block = 2222;
	dir_entry.size = 333;
	write_dir_entry(ROOT_BLOCK, 1, &dir_entry);

	memset((char *)dir_entry.filename, 0, sizeof(struct dir_entry_s));
	strcpy((char *)dir_entry.filename, "file1");
	dir_entry.attributes = 0x01;
	dir_entry.first_block = 3333;
	dir_entry.size = 444;
	write_dir_entry(ROOT_BLOCK, 2, &dir_entry);

	/* list entries from the root directory */
	for (i = 0; i < DIR_ENTRIES; i++) {
		read_dir_entry(ROOT_BLOCK, i, &dir_entry);
		printf("Entry %d, file: %s attr: %d first: %d size: %d\n", i, dir_entry.filename, dir_entry.attributes, dir_entry.first_block, dir_entry.size);
	}

	return 0;
}
