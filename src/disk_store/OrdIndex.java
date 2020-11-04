package disk_store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ordered index.  Duplicate search key values are allowed,
 * but not duplicate index table entries.  In DB terminology, a
 * search key is not a superkey.
 * 
 * A limitation of this class is that only single integer search
 * keys are supported.
 *
 */


public class OrdIndex implements DBIndex {
	
	private class Entry {
		int key;
		ArrayList<BlockCount> blocks;

		@Override
		public String toString() {
			StringBuilder newString = new StringBuilder();
			newString.append("Key: " + key + "\nBlocks { \n");
			for(int i = 0; i < blocks.size(); i++) {
				BlockCount current = blocks.get(i);
				newString.append(i + ": No. " + current.blockNo + "; Count: " + current.count + '\n');
			}
			newString.append('}');
			return newString.toString();
		}

		/**
		 * A function that will return all block numbers saved in for each block in the blocks array.
		 * @return - blockNums, an array containing all the blockNos for the blocks in our array.
		 */
		ArrayList<Integer> relatedBlocks() {
			ArrayList<Integer> blockNums = new ArrayList<>();
			for(BlockCount block : blocks) {
				blockNums.add(block.blockNo);
			}

			return blockNums;
		}

		/**
		 * Will search for a block who's blockNo is equal to target. If we find it, there are two possible results. If
		 * the count for blockNo is bigger than 1, then we decrement the count for the block. If the count is 1, we
		 * delete the block altogether. If we were able to delete something, we return true. Else, we return false.
		 * @param target - the blockNo for the block we are looking for.
		 * @return
		 */
		boolean removeFromBlock(int target) {
			for(int i = 0; i < blocks.size(); i++) {
				BlockCount curBlock = blocks.get(i);
				if(curBlock.blockNo == target) {
					if(curBlock.count > 1) {
						curBlock.count -= 1;
					} else {
						blocks.remove(i);
					}
					return true;
				}
			}

			return false;
		}
	}
	
	private class BlockCount {
		int blockNo;
		int count;
	}
	
	ArrayList<Entry> entries;
	int size = 0;
	
	/**
	 * Create an new ordered index.
	 */
	public OrdIndex() {
		entries = new ArrayList<>();
	}

	/**
	 * In this function, we use binary search to find the index of the entry we are looking for. We search for the key.
	 * @param key - the key we are searching for.
	 * @return - The index of the entry we are looking for. If key is not found, a -1 is returned.
	 */
	int indexOfKey(int key) {
		//If entries list id empty.
		if(entries.size() == 0) {
			return -1;
		}
		int low, middle, high;

		low = 0;
		high = entries.size() - 1;
		//if the key is less then the smallest key or larger than the biggest key.
		if (key < entries.get(low).key || key > entries.get(high).key) {
			return -1;
		}
		//We commence our binary search
		while (high - low > 1) {
			middle = (low + high) / 2;
			Entry middleEntry = entries.get(middle);
			if (middleEntry.key == key) {
				//we've found the entry with the correct key
				return middle;
			}
			else if (key < middleEntry.key) {
				high = middle;
			}
			else {
				low = middle;
			}
		}
		//our final checkers, just in case the entry was the low or the high the whole time
		if (entries.get(low).key == key) {
			return low;
		}
		else if (entries.get(high).key == key) {
			return high;
		}
		else {
			//We couldn't find the key at all
			return -1;
		}
	}

	/**
	 * This function will return then index where we need to insert our new entry. Our goal is to keep all the keys in
	 * the correct order.
	 * @param key - The key for the new entry we need to insert.
	 * @return - the location where we will be inserting.
	 */
	int findLocationToInsert(int key) {
		int low = 0, high = entries.size() - 1, middle;

		//If there entries array is empty or the key is smaller than the the smallest key in our list.
		if(entries.size() == 0 || key < entries.get(0).key) {
			return 0;
		}
		//If our key is bigger then the largest key in our entries.
		if(key > entries.get(high).key) {
			return entries.size();
		}

		while(high - low > 1) {
			middle = (low + high) / 2;
			Entry middleEntry = entries.get(middle);
			Entry middleMinus1 = entries.get(middle - 1);
			//if they key fits in between the middle key and the key for the entry right below it, then we know we need
			//to insert at middle
			if (middleEntry.key > key && middleMinus1.key < key) {
				return middle;
			}
			else if (key < middleEntry.key) {
				high = middle;
			}
			else {
				low = middle;
			}
		}
		//Our final checker. We need to either insert at the low or at the high.
		if (entries.get(low).key > key && entries.get(low - 1).key < key) {
			return low;
		}
		return high;
	}


	@Override
	public List<Integer> lookup(int key) {
		int indexOfKey = indexOfKey(key);
		if(indexOfKey == -1) {
			//If we couldn't find the key, we return an empty list.
			return Collections.emptyList();
		}
		//We return a list with all the related block number for the entry.
		return entries.get(indexOfKey).relatedBlocks();
	}

	@Override
	public void insert(int key, int blockNum) {
		size += 1;
		int index = indexOfKey(key); //The location where our key is in our entries array
		if(index != -1) {
			//if we found a location, we insert into an existing key
			insertIntoExistingKey(index, blockNum);
			return;
		}
		int insertIdx = findLocationToInsert(key); //we find where we need to insert our new key
		//We create our new entry
		Entry newEntry = new Entry();
		BlockCount newBlock = new BlockCount();
		newBlock.blockNo = blockNum;
		newBlock.count = 1;
		newEntry.key = key;
		newEntry.blocks = new ArrayList<>();
		newEntry.blocks.add(newBlock);

		if (insertIdx == entries.size() || entries.isEmpty()) {
			//If the entry needs to go at the end or if the entry is empty, we just add
			entries.add(newEntry);

			return;
		}
		//We add at the location we found earlier
		entries.add(insertIdx, newEntry);
	}

	@Override
	public void delete(int key, int blockNum) {
		// lookup key
		//  if key not found, should not occur.  Ignore it.
		//  decrement count for blockNum.
		//  if count is now 0, remove the blockNum.
		//  if there are no block number for this key, remove the key entry.
		//We find the location of the key
		int keyLocation = indexOfKey(key);
		//We get the entry
		Entry modifyEntry = entries.get(keyLocation);
		boolean hadToRemove = modifyEntry.removeFromBlock(blockNum);
		//If the entry has no more blocks, we delete the entry
		if(modifyEntry.blocks.size() == 0) {
			entries.remove(keyLocation);
		}
		//If we removed something, we decrement the size
		if(hadToRemove) {
			size--;
		}

	}

	/**
	 * This function will perform an insert into a key that already exists
	 * @param index - the index of the entry we want to insert into
	 * @param blockNum - the blockNo we are going to be inserting into
	 */
	void insertIntoExistingKey(int index, int blockNum) {
		Entry correctEntry = entries.get(index);
		for(BlockCount block : correctEntry.blocks) {
			//if we find that the blockNo is matches with a block in our entry, we just increment the count of said
			//block
			if(block.blockNo == blockNum) {
				block.count += 1;
				return;
			}
		}

		//If we didn't find a block with the blockNo, we create a new block and add it into our entry
		BlockCount newBlock = new BlockCount();
		newBlock.count = 1;
		newBlock.blockNo = blockNum;
		correctEntry.blocks.add(newBlock);
	}

	/**
	 * Return the number of entries in the index
	 * @return
	 */
	public int size() {
		return size;
		// you may find it useful to implement this
		
	}
	
	@Override
	public String toString() {
		throw new UnsupportedOperationException();
	}
}