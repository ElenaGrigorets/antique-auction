package com.antique.auction.controllers;

import com.antique.auction.models.Item;
import com.antique.auction.models.ItemPrice;
import com.antique.auction.services.ItemPriceService;
import com.antique.auction.services.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
public class ItemsController {
    private final ItemService itemService;
    private final ItemPriceService itemPriceService;

    @Value("${user.home}")
    public String uploadDir;

    @Autowired
    public ItemsController(ItemService itemsService, ItemPriceService itemPriceService) {
        this.itemService = itemsService;
        this.itemPriceService = itemPriceService;
    }

    @GetMapping(value = "/items")
    public ModelAndView getItems(ModelAndView modelAndView,
                                 @RequestParam("page") Optional<Integer> page,
                                 @RequestParam("size") Optional<Integer> size,
                                 @RequestParam("searchParam") Optional<String> searchParam,
                                 @RequestParam("sortOrder") Optional<String> sortOrder) {

        return populateModelAndView(modelAndView, page, size, searchParam, sortOrder);
    }

    @GetMapping(value = "/item/details/{id}")
    public ModelAndView gotToItemsDetail(@PathVariable int id) {
        Item item = itemService.findById(id);
        ModelAndView modelAndView = new ModelAndView("item-details", "item", item);
        return modelAndView;
    }

    @PostMapping(value = "/item/bid/add/{id}")
    public ModelAndView addBid(@PathVariable int id, Item item) {
        Item existingItem;
        if (item.getId() != null) {
            existingItem = itemService.findById(id);
        } else {
            existingItem = new Item();
        }
        if (item.getCurrentPrice() <= (existingItem != null ? existingItem.getCurrentPrice() : 0)) {
            ModelAndView modelAndView = new ModelAndView("item-details", "item", existingItem);
            modelAndView.addObject("wrongBid", "true");
            return modelAndView;
        }
        if (existingItem != null) {
            existingItem.setCurrentPrice(item.getCurrentPrice());
        }
        ItemPrice currentItemPrice = new ItemPrice();
        currentItemPrice.setItem(existingItem);
        currentItemPrice.setPriceValue(item.getCurrentPrice());
        if (existingItem != null) {
            existingItem.getItemPrices().add(currentItemPrice);
        }
        itemPriceService.save(currentItemPrice);
        itemService.save(existingItem);
        return populateModelAndView(new ModelAndView(), Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());
    }

    @RequestMapping(value = {"/", "/home"})
    public ModelAndView home(ModelAndView modelAndView) {
        if (itemService.count() == 0) {
            addInitialDemoData();
        }
        return populateModelAndView(modelAndView, Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());
    }


    @PostMapping(value = "/item/addEdit/{id}")
    public ModelAndView editItem(@PathVariable int id, Item item, @RequestParam ("file") MultipartFile file) {
        setImagePathToItem(item, file);
        fixItemDoubleDateParam(item);
        itemService.save(item);
        return populateModelAndView(new ModelAndView(), Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());
    }

    @PostMapping(value = "/item/addEdit")
    public ModelAndView addItem(Item item, @RequestParam ("file") MultipartFile file) {
        //todo multiple values are passed here... To check and fix
        fixItemDoubleDateParam(item);
        setImagePathToItem(item, file);
        itemService.save(item);
        return populateModelAndView(new ModelAndView(), Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());
    }

    private void setImagePathToItem(Item item, @RequestParam("file") MultipartFile file) {
        if (!file.isEmpty()) {
            try {
                String filename = file.getOriginalFilename();
                Path copyLocation = Paths
                        .get(uploadDir + File.separator + StringUtils.cleanPath(file.getOriginalFilename()));
                Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);
                item.setImageName(filename);
            } catch (IOException | RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping(value = "/item/delete/{id}")
    public ModelAndView addItem(@PathVariable Integer id) {
        itemService.deleteById(id);
        return populateModelAndView(new ModelAndView(), Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());
    }

    @GetMapping("/item/edit/view/{id}")
    public ModelAndView editItem(@PathVariable Integer id) {
        ModelAndView modelAndView = new ModelAndView();
        Item existingItem = itemService.findById(id);
        modelAndView.addObject("item", existingItem);
        modelAndView.setViewName("add-edit-item");
        return modelAndView;
    }

    @GetMapping("/item/add/view")
    public ModelAndView addItem() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject(new Item());
        modelAndView.setViewName("add-edit-item");
        return modelAndView;
    }

    @Autowired
    ResourceLoader resourceLoader;

    @GetMapping(value = "/items/images/{id}")
    public @ResponseBody byte[] getImage(@PathVariable Integer id) throws IOException {
        Item item = itemService.findById(id);
        BufferedImage img = null;
        try {
            String imageName = item.getImageName();
            if (imageName != null && !imageName.isEmpty()) {
                img = ImageIO.read(new File(uploadDir + "/" + imageName));
            }
        } catch (IOException ignored) {
        }
        return toByteArrayAutoClosable(img, "png");
    }
    private static byte[] toByteArrayAutoClosable(BufferedImage image, String type) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()){
            if (image != null) {
                ImageIO.write(image, type, out);
            }
            return out.toByteArray();
        }
    }

    private ModelAndView populateModelAndView(ModelAndView modelAndView, @RequestParam("page") Optional<Integer> page,
                                              @RequestParam("size") Optional<Integer> size, Optional<String> searchParam, Optional<String> sortOrder) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(10);

        Page<Item> itemPage = itemService.findPaginated(PageRequest.of(currentPage - 1, pageSize), searchParam, sortOrder);
        modelAndView.addObject("itemPage", itemPage);

        int totalPages = itemPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            modelAndView.addObject("pageNumbers", pageNumbers);
        }
        sortOrder.ifPresent(s -> modelAndView.addObject("sortOrder", s));
        searchParam.ifPresent(s -> modelAndView.addObject("searchParam", s));
        modelAndView.setViewName("home");
        return modelAndView;
    }

    private void fixItemDoubleDateParam(Item item) {
        int indexOfComma = item.getDateString().indexOf(',');
        if (indexOfComma != -1) {
            item.setDateString(item.getDateString().substring(0, indexOfComma));
        }
    }


    private void addInitialDemoData() {
        itemService.save(getItem("demoItem1", "demo description 1", 10));
        itemService.save(getItem("demoItem2", "demo description 21", 20));
        itemService.save(getItem("demoItem3", "demo description 31", 100));
        itemService.save(getItem("demoItem4", "demo description 41", 15));
        itemService.save(getItem("demoItem5", "demo description 51", 10));
        itemService.save(getItem("demoItem6", "demo description 61", 35));
        itemService.save(getItem("demoItem7", "demo description 71", 11));
        itemService.save(getItem("demoItem8", "demo description 81", 30));
        itemService.save(getItem("demoItem9", "demo description 91", 10));
        itemService.save(getItem("demoItem10", "demo description 101", 10));
        itemService.save(getItem("demoItem11", "demo description 102", 55));
    }

    private Item getItem(String name, String description, int price) {
        Item item = new Item();
        item.setName(name);
        item.setDescription(description);
        item.setCurrentPrice(price);
        item.setImageName(name + ".png");
        return item;
    }

}
