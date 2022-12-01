import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Stack;
/**
 * Wfc_1d
 */
public class Wfc_1d {
    static class TileData { // class lưu các điều kiện kể của mỗi kiểu ô
        HashMap<Tile, Boolean> left;
        HashMap<Tile, Boolean> right;

        public TileData(HashMap<Tile, Boolean> left, HashMap<Tile, Boolean> right) {
            this.left = left;
            this.right = right;
        }
    }
    
    enum Tile {RED, YELLOW, GREEN}; // danh sách các kiểu ô có thể xếp
    static final int TILE_COUNT = 3; // số lượng các kiểu ô
    static HashMap<Tile, TileData> tileDatas;

    static Tile[] map; // array lưu kết quả cuối cùng
    static ArrayList<HashMap<Tile, Boolean>> wave; // hàm sóng, chứa tất cả các khả năng của mỗi ô
    static int[] entropy; // entropy của mỗi ô
    static int placed_tiles;

    static Random rd;

    public static void main(String[] args) {
        rd = new Random();
        waveFunctionCollapse(50);
    }

    // chạy thuật toán
    static void waveFunctionCollapse(int map_size) {
        //bước 1: khởi tạo
        initialize(map_size);
        initAdjacencies();

        //bước 2: lặp
        while(true) {
            Integer min_idx = getMinEntropyIdx();
            if(min_idx == null) { // nếu không còn ô nào có entropy khác 0, thoát khỏi vòng lặp
                break;
            }

            observe(min_idx); //2.1: quan sát
            propogate(min_idx); //2.2: lan truyền
        }
    }

    // khởi tạo hàm sóng, entropy, bản đồ
    static void initialize(int map_size) {
        map = new Tile[map_size];
        wave = new ArrayList<HashMap<Tile, Boolean>>();
        entropy = new int[map_size];
        HashMap<Tile, Boolean> temp;
        
        // khởi tạo hàm sóng, mọi hệ số boolean được đặt là true
        for (int i = 0; i < map_size; i++) {
            temp = new HashMap<Tile, Boolean>();
            for (Tile t : Tile.values()) {
                temp.put(t, true);
            }
            wave.add(temp);
        }

        // khởi tạo entropy, mọi ô đều có entropy bằng số các kiểu ô
        for (int i = 0; i < entropy.length; i++) {
            entropy[i] = TILE_COUNT;
        }
    }

    // đặt điều kiện kề cho các kiểu ô
    static void initAdjacencies() {
        tileDatas = new HashMap<Tile, TileData>();
        HashMap<Tile, Boolean> temp_left;
        HashMap<Tile, Boolean> temp_right;

        // đỏ được gần đỏ và vàng, không được gần xanh
        temp_left = new HashMap<Tile, Boolean>();
        temp_right = new HashMap<Tile, Boolean>();

        temp_left.put(Tile.RED, true);
        temp_left.put(Tile.YELLOW, true);
        temp_left.put(Tile.GREEN, false);
        temp_right.put(Tile.RED, true);
        temp_right.put(Tile.YELLOW, true);
        temp_right.put(Tile.GREEN, false);

        tileDatas.put(Tile.RED, new TileData(temp_left, temp_right));

        // vàng được ở gần mọi ô
        temp_left = new HashMap<Tile, Boolean>();
        temp_right = new HashMap<Tile, Boolean>();

        temp_left.put(Tile.RED, true);
        temp_left.put(Tile.YELLOW, true);
        temp_left.put(Tile.GREEN, true);
        temp_right.put(Tile.RED, true);
        temp_right.put(Tile.YELLOW, true);
        temp_right.put(Tile.GREEN, true);

        tileDatas.put(Tile.YELLOW, new TileData(temp_left, temp_right));
        
        // xanh được gần xanh và vàng, không được gần đỏ
        temp_left = new HashMap<Tile, Boolean>();
        temp_right = new HashMap<Tile, Boolean>();

        temp_left.put(Tile.RED, false);
        temp_left.put(Tile.YELLOW, true);
        temp_left.put(Tile.GREEN, true);
        temp_right.put(Tile.RED, false);
        temp_right.put(Tile.YELLOW, true);
        temp_right.put(Tile.GREEN, true);

        tileDatas.put(Tile.GREEN, new TileData(temp_left, temp_right));
    }

    // tìm ô có entropy khác 0 nhỏ nhất
    static Integer getMinEntropyIdx() {
        int result = -1;
        for (int i = 0; i < entropy.length; i++) {
            if (entropy[i] == 1) {
                continue;
            }
            if (result == -1) {
                result = i;
                continue;
            }
            if (entropy[i] < entropy[result]) {
                result = i;
            }
        }

        if (result == -1) {
            return null;
        }
        return result;
    }

    // hàm quan sát: sụp đổ trạng thái của ô được chọn thành 1 kiểu ô cố định
    static void observe(int min_idx) {
        ArrayList<Tile> validTiles = getPossibleTiles(min_idx); //tìm các kiểu ô có thể xuất hiện tại ô đó

        Tile chosen_tile = validTiles.get(rd.nextInt(validTiles.size())); //sụp đổ
        validTiles.remove(chosen_tile);

        for (Tile tile : validTiles) { // thay các hệ số boolean khác bằng false
            wave.get(min_idx).replace(tile, false);
        }
        entropy[min_idx] = 1;
    }

    // tìm vị trí các ô bị ảnh hưởng bởi ô vừa bị sụp đổ
    static ArrayList<Integer> getValidDirs(int idx) {
        ArrayList<Integer> result = new ArrayList<Integer>(2);
        if (idx > 0) {
            result.add(-1);
        }
        if (idx < map.length - 1) {
            result.add(1);
        }
        return result;
    }

    // lan truyền tác động của việc sụp đổ
    static void propogate(int min_idx) {
        Stack<Integer> stack = new Stack<Integer>();
        stack.add(min_idx);

        while (!stack.isEmpty()) {
            int current_idx = stack.pop();
            ArrayList<Tile> current_tile_possibilities = getPossibleTiles(current_idx);

            for (int dir : getValidDirs(current_idx)) {
                int affected_idx = current_idx + dir;
                if (entropy[affected_idx] == 1) {
                    continue;
                }
                for (Tile tile : Tile.values()) {
                    boolean tile_allowed = false;

                    for (Tile poss_tile : current_tile_possibilities) {
                        HashMap<Tile, Boolean> adj_conds = getAdjacencyConditions(poss_tile, dir);
                        if (adj_conds.get(tile)) {
                            tile_allowed = true;
                            break;
                        }
                    }

                    boolean coeff = wave.get(affected_idx).get(tile);
                    if (coeff && !tile_allowed) {
                        wave.get(affected_idx).replace(tile, false);
                        --entropy[affected_idx];
                        if (!stack.contains(affected_idx)) {
                                stack.add(affected_idx);
                        }
                    }
                }
            }
            updateMap();
        }
    }

    // trả về ràng buộc của kiểu ô đã cho, tùy theo hướng trái/phải
    static HashMap<Tile, Boolean> getAdjacencyConditions(Tile tile, int dir) {
        if (dir == -1) {
            return tileDatas.get(tile).left;
        }
        if (dir == 1) {
            return tileDatas.get(tile).right;
        } else {
            return null;
        }
    }

    // tìm các kiểu ô chưa bị cấm xuất hiện tại vị trí được cho
    static ArrayList<Tile> getPossibleTiles(int idx) {
        ArrayList<Tile> validTiles = new ArrayList<Tile>();
        boolean temp;
        for (Tile t : Tile.values()) {
            temp = wave.get(idx).get(t);
            if (temp == true) {
               validTiles.add(t);
            }
        }
        return validTiles;
    }

    // cập nhật bản đồ
    static void updateMap() {
        for (int i = 0; i < map.length; i++) {
            if (entropy[i] != 1) {
                continue;
            }
            for (Tile tile : Tile.values()) {
                boolean coeff = wave.get(i).get(tile);
                if (coeff) {
                    map[i] = tile;
                    break;
                }
            }
        }
        printMap();
    }

    static int updates = 0;
    static void printMap() {
        updates++;
        System.out.println("Iteration: " + updates);

        int idx = -1;
        for (Tile i : map) {
            idx++;
            if (i == null) {
                System.out.print(entropy[idx]);
                continue;
            }
            switch (i) {
                case RED:
                    System.out.print("R");
                    break;
                case GREEN:
                    System.out.print("G");
                    break;
                case YELLOW:
                    System.out.print("Y");
                    break;
                default:
                    break;
            }
        }
        System.out.println();
    }
}