module auction.shared {
    exports aution_shared.base;
    exports aution_shared.entities;
    exports aution_shared.interfaces;
    exports aution_shared.items;
    exports aution_shared.behaviors;

    opens aution_shared.entities to auction.server, auction.client;
}