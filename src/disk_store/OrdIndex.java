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

		ArrayList<Integer> relatedBlocks() {
			ArrayList<Integer> blockNums = new ArrayList<>();
			for(BlockCount block : blocks) {
				blockNums.add(block.blockNo);
			}

			return blockNums;
		}

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

	int indexOfKey(int key) {
		if(entries.size() == 0) {
			return -1;
		}
		int low, middle, high;

		low = 0;
		high = entries.size() - 1;
		if (key < entries.get(low).key || key > entries.get(high).key) {
			return -1;
		}
		while (high - low > 1) {
			middle = (low + high) / 2;
			Entry middleEntry = entries.get(middle);
			if (middleEntry.key == key) {
				return middle;
			}
			else if (key < middleEntry.key) {
				high = middle;
			}
			else {
				low = middle;
			}
		}
		if (entries.get(low).key == key) {
			return low;
		}
		else if (entries.get(high).key == key) {
			return high;
		}
		else {
			return -1;
		}
	}

	int findLocationToInsert(int key) {
		int low = 0, high = entries.size() - 1, middle;
		if(entries.size() == 0 || key < entries.get(0).key) {
			return 0;
		}
		if(key > entries.get(high).key) {
			return entries.size();
		}

		while(high - low > 1) {
			middle = (low + high) / 2;
			Entry middleEntry = entries.get(middle);
			Entry middleMinus1 = entries.get(middle - 1);
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
		if (entries.get(low).key > key && entries.get(low - 1).key < key) {
			return low;
		}
		return high;
	}


	@Override
	public List<Integer> lookup(int key) {
		int indexOfKey = indexOfKey(key);
		if(indexOfKey == -1) {
			return Collections.emptyList();
		}
		return entries.get(indexOfKey).relatedBlocks();
	}

	@Override
	public void insert(int key, int blockNum) {
		size += 1;
		int index = indexOfKey(key);
		if(index != -1) {
			insertIntoExistingKey(index, blockNum);
			return;
		}
		int insertIdx = findLocationToInsert(key);
		Entry newEntry = new Entry();
		BlockCount newBlock = new BlockCount();
		newBlock.blockNo = blockNum;
		newBlock.count = 1;
		newEntry.key = key;
		newEntry.blocks = new ArrayList<>();
		newEntry.blocks.add(newBlock);
		entries.add(insertIdx, newEntry);
	}

	@Override
	public void delete(int key, int blockNum) {
		// lookup key
		//  if key not found, should not occur.  Ignore it.
		//  decrement count for blockNum.
		//  if count is now 0, remove the blockNum.
		//  if there are no block number for this key, remove the key entry.
		int keyLocation = indexOfKey(key);
		Entry modifyEntry = entries.get(keyLocation);
		boolean hadToRemove = modifyEntry.removeFromBlock(blockNum);
		if(modifyEntry.blocks.size() == 0) {
			entries.remove(key);
		}
		if(hadToRemove) {
			size--;
		}

	}

	void insertIntoExistingKey(int index, int blockNum) {
		Entry correctEntry = entries.get(index);
		for(BlockCount block : correctEntry.blocks) {
			if(block.blockNo == blockNum) {
				block.count += 1;
				return;
			}
		}

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