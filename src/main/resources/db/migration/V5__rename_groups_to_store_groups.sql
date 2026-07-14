-- Rename the physical table to avoid MySQL reserved keyword conflicts in Hibernate DDL/validation.
RENAME TABLE `groups` TO store_groups;
