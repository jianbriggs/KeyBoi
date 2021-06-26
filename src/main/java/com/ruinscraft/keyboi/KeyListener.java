package com.ruinscraft.keyboi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.Sign;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Door;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.map.MapView;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import net.md_5.bungee.api.ChatColor;

public class KeyListener implements Listener{
	private KeyBoi plugin;
	
	private final String LOCK_SIGN_IDENTIFIER = ChatColor.DARK_GREEN + "[Key]";
	private final String LOCK_SIGN_IDENTIFIER_NO_COLOR = "[Key]";
	
    public KeyListener(KeyBoi plugin) {
    	this.plugin = plugin;
    }
    
    @EventHandler
    public void onSignChangeEvent(SignChangeEvent evt) {
    	Player player = evt.getPlayer();
    	Block block = evt.getBlock();
    	
    	if(block.getState() instanceof Sign) {
	    	if(validateKeySignEntry(evt.getLines())) {
	    		evt.setLine(0, LOCK_SIGN_IDENTIFIER);
	    		
	    		Sign s = (Sign) block.getState();
	    		
	    		DataManager dm = new DataManager(plugin);
	    		
	    		if(dm.setNewSignKeyTags(player, s)){
		    		if(player.isOnline()) {
		    			player.sendMessage(ChatColor.GREEN + "Block locked with key sign! Right-click the sign to set the key.");
		    		}
	    		}
	    		else {
	    			if(player.isOnline()) {
	    				player.sendMessage(ChatColor.RED + "Unable to set key values for lock.");
	    			}
	    		}
	    	}
    	}
    }
    
    /**
     * This function handles all Player interactions with a locked blocks.
     * @param evt - called Player interact event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent evt) {
        Player player = evt.getPlayer();

        if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
        	Block clickedBlock = evt.getClickedBlock();
        	BlockState state = clickedBlock.getState();
        	
        	if (blockIsLockable(clickedBlock)) {
        		Sign signInfo = blockHasKeySign(clickedBlock);
        		
        		if(signInfo != null) {
        			// TODO: debug
        			player.sendMessage("(Debug) Found a KeyBoi sign attached to block");
        			
        			DataManager dm = new DataManager(plugin);
        			
        			PersistentDataContainer pdc = signInfo.getPersistentDataContainer();
        			
        			if(dm.containerHasKeyTags(pdc)) {
        				// TODO: debug
            			player.sendMessage("(Debug) Yo! Found some KeyBoi data!");
        			}
        		}
        	}
        	else if(blockIsSign(clickedBlock)){
        		Sign sign = (Sign) state;
        		if(signIsKeySign(sign)) {
        			if(player.isOnline()) {
        				// TODO: debug
        				player.sendMessage("(Debug) You clicked on a KeyBoi sign");
        			}
        		}
        	}
        }
    }
    
    private Sign blockHasKeySign(Block block) {
    	List<Block> surroundingBlocks = new ArrayList<Block>();
    	
    	surroundingBlocks.add(block.getRelative(BlockFace.NORTH));
    	surroundingBlocks.add(block.getRelative(BlockFace.SOUTH));
    	surroundingBlocks.add(block.getRelative(BlockFace.EAST));
    	surroundingBlocks.add(block.getRelative(BlockFace.WEST));
    	surroundingBlocks.add(block.getRelative(BlockFace.UP));
    	surroundingBlocks.add(block.getRelative(BlockFace.DOWN));
    	
    	for(Block b : surroundingBlocks) {
    		if(blockIsSign(b)) {
    			Sign s = (Sign) b.getState();
    			
    			if(signIsKeySign(s)) {
    				return s;
    			}
    		}
    	}
    	
    	return null;
    }
    
    private void lockBlock(Player player, ItemStack key, Sign s) {
    	DataManager dm = new DataManager(plugin);
    	
    	if(dm.setKeyTags(player, key, s)) {
    		if(player.isOnline()) {
    			player.sendMessage("Block successfully locked with key!");
    		}
    	}
    	else {
    		if(player.isOnline()) {
    			player.sendMessage("Unable to lock block with key");
    		}
    	}
    }
    

    private boolean stateHasKeyMetadata(BlockState state) {
    	return state.hasMetadata("keyboi-locked")
    		&& state.hasMetadata("keyboi-keyname")
    		&& state.hasMetadata("keyboi-keymaterial")
    		&& state.hasMetadata("keyboi-keycreator")
    		&& state.hasMetadata("keyboi-hash");
    		//&& state.hasMetadata("keyboi-whoplacedblock");
    }
    
    private List<MetadataValue> stateGetKeyMetadata(BlockState state) {
    	List<MetadataValue> keymeta = new ArrayList<MetadataValue>();
    	
    	keymeta.add(state.getMetadata("keyboi-locked").get(0));
    	keymeta.add(state.getMetadata("keyboi-keyname").get(0));
    	keymeta.add(state.getMetadata("keyboi-keymaterial").get(0));
    	keymeta.add(state.getMetadata("keyboi-keycreator").get(0));
    	//keymeta.add(state.getMetadata("keyboi-whoplacedblock").get(0));
    	
    	return keymeta;
    }
    private boolean blockIsLockable(Block block) {
    	return blockIsDoor(block) || blockIsTrapdoor(block) || blockIsGate(block) || blockIsStorage(block);
    }
    
    private boolean blockIsSign(Block block) {
    	return block != null && (
    		    block.getType().equals(Material.ACACIA_WALL_SIGN)
    		 || block.getType().equals(Material.BIRCH_WALL_SIGN)
    		 || block.getType().equals(Material.CRIMSON_WALL_SIGN)
    		 || block.getType().equals(Material.DARK_OAK_WALL_SIGN)
    		 || block.getType().equals(Material.JUNGLE_WALL_SIGN)
    		 || block.getType().equals(Material.OAK_WALL_SIGN)
    		 || block.getType().equals(Material.SPRUCE_WALL_SIGN)
    		 || block.getType().equals(Material.WARPED_WALL_SIGN)
    		 );
    }
    
	private boolean blockIsDoor(Block block) {
		return block != null && (
    		    block.getType().equals(Material.ACACIA_DOOR)
    		 || block.getType().equals(Material.BIRCH_DOOR)
    		 || block.getType().equals(Material.CRIMSON_DOOR)
    		 || block.getType().equals(Material.DARK_OAK_DOOR)
    		 || block.getType().equals(Material.IRON_DOOR)
    		 || block.getType().equals(Material.JUNGLE_DOOR)
    		 || block.getType().equals(Material.OAK_DOOR)
    		 || block.getType().equals(Material.SPRUCE_DOOR)
    		 || block.getType().equals(Material.WARPED_DOOR)
    		 );
	}
    
	private boolean blockIsTrapdoor(Block block) {
		return block != null && (
    		    block.getType().equals(Material.ACACIA_TRAPDOOR)
    		 || block.getType().equals(Material.BIRCH_TRAPDOOR)
    		 || block.getType().equals(Material.CRIMSON_TRAPDOOR)
    		 || block.getType().equals(Material.DARK_OAK_TRAPDOOR)
    		 || block.getType().equals(Material.IRON_TRAPDOOR)
    		 || block.getType().equals(Material.JUNGLE_TRAPDOOR)
    		 || block.getType().equals(Material.OAK_TRAPDOOR)
    		 || block.getType().equals(Material.SPRUCE_TRAPDOOR)
    		 || block.getType().equals(Material.WARPED_TRAPDOOR)
    		 );
	}
	
	private boolean blockIsGate(Block block) {
		return block != null && (
    		    block.getType().equals(Material.ACACIA_FENCE_GATE)
    		 || block.getType().equals(Material.BIRCH_FENCE_GATE)
    		 || block.getType().equals(Material.CRIMSON_FENCE_GATE)
    		 || block.getType().equals(Material.DARK_OAK_FENCE_GATE)
    		 || block.getType().equals(Material.JUNGLE_FENCE_GATE)
    		 || block.getType().equals(Material.OAK_FENCE_GATE)
    		 || block.getType().equals(Material.SPRUCE_FENCE_GATE)
    		 || block.getType().equals(Material.WARPED_FENCE_GATE)
    		 );
	}
    /**
     * Checks whether a block is a chest, double chest, or barrel
     * (note that Enderchests are not checked)
     * @param block - Block to check
     * @return True if block is chest, False otherwise
     */
    private boolean blockIsStorage(Block block) {
    	return block != null && (
    			block.getState() instanceof Chest
    		 || block.getState() instanceof DoubleChest
    		 || block.getState() instanceof Barrel
    		 );
    }
    
    private boolean signIsKeySign(Sign s) {
    	return s.getLine(0).equalsIgnoreCase(LOCK_SIGN_IDENTIFIER);
    }
    private boolean validateKeySignEntry(String[] lines) {
    	return lines[0].equalsIgnoreCase(LOCK_SIGN_IDENTIFIER_NO_COLOR);
    }
    
    private String materialPrettyPrint(Material material) {
    	String[] words = material.toString().split("_");
    	String output = "";
    	
    	for( String word : words) {
    		output += word.substring(0,1).toUpperCase() + word.substring(1).toLowerCase() + " ";
    	}
    	output = output.trim();
    	return output;
    }
    
	private String truncateText(String message) {
    	if(message.length() >= 38) {
    		return message.substring(0, 34) + "...";
    	}
    	else {
    		return message;
    	}
    }
    
    private String prettyPrint(String message) {
    	String[] words = message.split("_");
    	String  output = "";
    	
    	for( String word : words) {
    		output += word.substring(0,1).toUpperCase() + word.substring(1).toLowerCase() + " ";
    	}
    	output = output.trim();
    	return output;
    }

    /**
     * Sends an error message to the player.
     * @param p Player to send message to
     * @param message Message to output
     */
    private void sendError(Player player, String message) {
    	if(player.isOnline()) {
    		player.sendMessage(ChatColor.RED + message);
    	}
    }

    private boolean itemIsFinishedBook(ItemStack item) {
    	return item != null && item.getType().equals(XMaterial.WRITTEN_BOOK.parseMaterial());
    }
    
    private boolean itemIsWritableBook(ItemStack item) {
    	return item != null && item.getType().equals(XMaterial.WRITABLE_BOOK.parseMaterial());
    }
    
    private boolean itemIsAir(ItemStack item) {
    	return item != null && item.getType().equals(XMaterial.AIR.parseMaterial());
    }
    
    private boolean itemIsBanner(ItemStack item) {
    	return item != null && item.getType().name().contains("BANNER");
    }
    
    private boolean itemIsShield(ItemStack item) {
    	return item != null && item.getType().equals(XMaterial.SHIELD.parseMaterial());
    }
    
    private boolean itemIsPotion(ItemStack item) {
		return item != null && item.getType().name().contains("POTION");
	}
    
    private boolean itemIsFilledMap(ItemStack item) {
    	return item != null && item.getType().equals(XMaterial.FILLED_MAP.parseMaterial());
    }
    
    private boolean itemIsShulkerBox(ItemStack item) {
    	return item != null && item.getType().name().contains("SHULKER_BOX");
    }
    
    private boolean itemIsEnchantedBook(ItemStack item) {
    	return item != null && item.getType().equals(XMaterial.ENCHANTED_BOOK.parseMaterial());
    }
    
    private boolean itemIsTippedArrow(ItemStack item) {
    	return item != null && item.getType().equals(XMaterial.TIPPED_ARROW.parseMaterial());
    }
    
    private boolean itemIsPolishedBlackstone(ItemStack item) {
    	return item != null && item.getType().name().contains("POLISHED_BLACKSTONE");
    }
    
    private boolean itemIsPlayerHead(ItemStack itemToBuy) {
		return itemToBuy != null && itemToBuy.getType().equals(Material.PLAYER_HEAD);
	}
}