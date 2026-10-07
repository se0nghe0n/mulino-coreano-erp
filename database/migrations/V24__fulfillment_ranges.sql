ALTER TABLE mulino_inventory_GenealogyEdges ADD COLUMN sourceStartQuantity numeric(38,12), ADD COLUMN targetStartQuantity numeric(38,12);
ALTER TABLE mulino_inventory_GenealogyEdges ADD CONSTRAINT genealogy_exact_offsets CHECK ((sourceStartQuantity IS NULL AND targetStartQuantity IS NULL) OR (sourceStartQuantity >= 0 AND targetStartQuantity >= 0));
