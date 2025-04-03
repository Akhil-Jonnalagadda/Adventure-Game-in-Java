import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

class StoryNode {
    String description;
    Map<String, String> choices;
    boolean requiresItem;
    String requiredItem;

    public StoryNode(String description) {
        this.description = description;
        this.choices = new HashMap<>();
        this.requiresItem = false;
        this.requiredItem = null;
    }

    public void addChoice(String choiceText, String nextNodeId) {
        choices.put(choiceText, nextNodeId);
    }

    public void setRequiredItem(String item) {
        this.requiresItem = true;
        this.requiredItem = item;
    }
}

class Enemy {
    String name;
    int health;
    int attackPower;

    public Enemy(String name, int health, int attackPower) {
        this.name = name;
        this.health = health;
        this.attackPower = attackPower;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void takeDamage(int damage) {
        health -= damage;
        if (health < 0) health = 0;
        System.out.println(name + " takes " + damage + " damage. HP left: " + health);
    }
}

class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    ArrayList<String> inventory;
    int health;
    int maxHealth;
    int attackPower;
    int level;
    int xp;

    public Player() {
        this.inventory = new ArrayList<>();
        this.health = 100;
        this.maxHealth = 100;
        this.attackPower = 10;
        this.level = 1;
        this.xp = 0;
    }

    public void addItem(String item) {
        inventory.add(item);
        System.out.println("You picked up: " + item);
    }

    public boolean hasItem(String item) {
        return inventory.contains(item);
    }

    public void removeItem(String item) {
        inventory.remove(item);
        System.out.println("You used: " + item);
    }

    public void showInventory() {
        if (inventory.isEmpty()) {
            System.out.println("Your inventory is empty.");
        } else {
            System.out.println("Inventory: " + inventory);
        }
    }

    public void changeHealth(int amount) {
        health += amount;
        if (health > maxHealth) health = maxHealth;
        if (health < 0) health = 0;
        System.out.println("Health " + (amount >= 0 ? "increased" : "decreased") + " by " + Math.abs(amount) + ". Current HP: " + health);
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void gainXP(int amount) {
        xp += amount;
        System.out.println("Gained " + amount + " XP. Total XP: " + xp);
        checkLevelUp();
    }

    private void checkLevelUp() {
        int xpForNextLevel = level * 50;
        if (xp >= xpForNextLevel) {
            level++;
            maxHealth += 20;
            health = maxHealth;
            attackPower += 5;
            System.out.println("Level Up! You are now Level " + level + ". Max HP: " + maxHealth + ", Attack: " + attackPower);
        }
    }
}

class Game {
    Map<String, StoryNode> storyMap;
    Player player;
    Scanner scanner;
    Random random;

    public Game() {
        storyMap = new HashMap<>();
        player = new Player();
        scanner = new Scanner(System.in);
        random = new Random();
        setupStory();
    }

    private void setupStory() {
        // Node 1: Start
        StoryNode node1 = new StoryNode("You wake up in a dark forest. A path leads north, and a rusty sword lies to your right.");
        node1.addChoice("Go north", "node2");
        node1.addChoice("Pick up the sword", "node3");
        storyMap.put("node1", node1);

        // Node 2: Encounter without sword
        StoryNode node2 = new StoryNode("You walk north and encounter a Goblin!");
        node2.addChoice("Fight", "node4");
        node2.addChoice("Run back", "node1");
        storyMap.put("node2", node2);

        // Node 3: Pick up sword
        StoryNode node3 = new StoryNode("You pick up the rusty sword, increasing your attack power.");
        node3.addChoice("Go north", "node5");
        storyMap.put("node3", node3);

        // Node 4: Combat without sword (weak)
        StoryNode node4 = new StoryNode("You face the Goblin with bare hands.");
        storyMap.put("node4", node4);

        // Node 5: Combat with sword
        StoryNode node5 = new StoryNode("You face the Goblin with your sword.");
        storyMap.put("node5", node5);

        // Node 6: After combat
        StoryNode node6 = new StoryNode("You defeated the Goblin! A locked gate is ahead, with a key on a branch.");
        node6.addChoice("Take the key", "node7");
        node6.addChoice("Go back", "node1");
        storyMap.put("node6", node6);

        // Node 7: Use key
        StoryNode node7 = new StoryNode("You have the key. The gate stands before you.");
        node7.addChoice("Use the key", "node8");
        node7.addChoice("Go back", "node1");
        storyMap.put("node7", node7);

        // Node 8: Good ending
        StoryNode node8 = new StoryNode("You unlock the gate and escape into a bright meadow! (Game Over)");
        node8.setRequiredItem("key");
        storyMap.put("node8", node8);

        // Node 9: Death ending
        StoryNode node9 = new StoryNode("You succumb to your wounds and collapse. (Game Over)");
        storyMap.put("node9", node9);
    }

    public void play() {
        String currentNodeId = "node1";

        while (player.isAlive()) {
            StoryNode currentNode = storyMap.get(currentNodeId);
            System.out.println("\n" + currentNode.description);
            System.out.println("Health: " + player.health + "/" + player.maxHealth + " | Level: " + player.level + " | XP: " + player.xp);

            if (currentNode.requiresItem && !player.hasItem(currentNode.requiredItem)) {
                System.out.println("You need a " + currentNode.requiredItem + " to proceed!");
                currentNodeId = "node7";
                continue;
            }

            if (currentNodeId.equals("node4") || currentNodeId.equals("node5")) {
                Enemy enemy = new Enemy("Goblin", 50, 15);
                if (fightEnemy(enemy, currentNodeId.equals("node5"))) {
                    player.gainXP(30);
                    currentNodeId = "node6";
                } else {
                    currentNodeId = "node9";
                }
                continue;
            }

            System.out.println("Type 'inventory', 'save', 'load', 'use <item>', or choose an option:");
            int choiceNum = 1;
            for (String choice : currentNode.choices.keySet()) {
                System.out.println(choiceNum + ". " + choice);
                choiceNum++;
            }

            String input = getTimedInput(10); // 10-second timer for choices
            if (input == null) {
                System.out.println("Time’s up! You hesitate too long.");
                continue;
            }

            input = input.trim().toLowerCase();
            if (input.equals("inventory")) {
                player.showInventory();
            } else if (input.equals("save")) {
                saveGame();
            } else if (input.equals("load")) {
                loadGame();
            } else if (input.startsWith("use ")) {
                String item = input.substring(4).trim();
                if (player.hasItem(item)) {
                    if (currentNodeId.equals("node7") && item.equals("key")) {
                        player.removeItem("key");
                        currentNodeId = "node8";
                    } else {
                        System.out.println("Can’t use " + item + " here.");
                    }
                } else {
                    System.out.println("You don’t have " + item + "!");
                }
            } else {
                try {
                    int choiceIndex = Integer.parseInt(input) - 1;
                    String[] choiceArray = currentNode.choices.keySet().toArray(new String[0]);
                    if (choiceIndex >= 0 && choiceIndex < choiceArray.length) {
                        String selectedChoice = choiceArray[choiceIndex];
                        currentNodeId = currentNode.choices.get(selectedChoice);
                        if (selectedChoice.equals("Pick up the sword")) {
                            player.addItem("sword");
                            player.attackPower += 10; // Sword boosts attack
                        } else if (selectedChoice.equals("Take the key")) {
                            player.addItem("key");
                        }
                        if (currentNodeId.equals("node8") && player.hasItem("key")) {
                            System.out.println(storyMap.get("node8").description);
                            break;
                        }
                    } else {
                        System.out.println("Invalid choice.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Enter a number, 'inventory', 'save', 'load', or 'use <item>'.");
                }
            }
        }

        if (!player.isAlive()) {
            System.out.println(storyMap.get("node9").description);
        }
        scanner.close();
        System.out.println("Thanks for playing!");
    }

    private boolean fightEnemy(Enemy enemy, boolean hasSword) {
        System.out.println("\nCombat begins! " + enemy.name + " (HP: " + enemy.health + ") vs You (HP: " + player.health + ")");
        while (enemy.isAlive() && player.isAlive()) {
            System.out.println("Choose action (10s): 1. Attack  2. Dodge");
            String action = getTimedInput(10);
            if (action == null) {
                System.out.println("You hesitated!");
                player.changeHealth(-enemy.attackPower);
                continue;
            }

            if (action.equals("1")) {
                int damage = hasSword ? player.attackPower : player.attackPower / 2;
                enemy.takeDamage(damage);
                if (enemy.isAlive()) {
                    player.changeHealth(-enemy.attackPower);
                }
            } else if (action.equals("2")) {
                if (random.nextInt(100) < 50) { // 50% dodge chance
                    System.out.println("You dodged the attack!");
                } else {
                    System.out.println("Dodge failed!");
                    player.changeHealth(-enemy.attackPower);
                }
            } else {
                System.out.println("Invalid action!");
                player.changeHealth(-enemy.attackPower);
            }
        }
        return player.isAlive();
    }

    private String getTimedInput(int seconds) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < seconds * 1000) {
            if (scanner.hasNextLine()) {
                return scanner.nextLine();
            }
            try {
                Thread.sleep(100); // Check every 100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    private void saveGame() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("game_save.dat"))) {
            oos.writeObject(player);
            System.out.println("Game saved successfully!");
        } catch (IOException e) {
            System.out.println("Failed to save game: " + e.getMessage());
        }
    }

    private void loadGame() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("game_save.dat"))) {
            player = (Player) ois.readObject();
            System.out.println("Game loaded successfully!");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Failed to load game: " + e.getMessage());
        }
    }
}

public class AdventureGame {
    public static void main(String[] args) {
        Game game = new Game();
        System.out.println("Welcome to the Forest Escape Adventure!");
        game.play();
    }
}