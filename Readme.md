
# Spatial Data - a scala library for spatial sensitivity analysis

## General purpose

Provide primitives, for territorial systems, to

 - generate synthetic spatial configurations possibly resembling real configurations
 - perturbate real datasets
 - compare synthetically generated configurations and real configurations

Implemented aspects for now are built environment grids (population or buildings) and road networks.

## Applications

The folder `library/data` contains the results of the application of the library to the calibration of building configurations generators.

## Building

In the `library` folder, using `sbt`:
 - `sbt assembly` creates a single jar including all dependencies (configure main class in build)
 - `sbt osgiBundle` creates an osgi bundle that can be used as an OpenMOLE plugin

## Architecture

Packages :
 - `grid` grid data methods
 - `network` network data methods
 - `points` points methods
 - `test` test classes
 - `utils` tools


Each data type package contains the following subpackages :
 - `synthetic` synthetic generators
 - `measures` indicators
 - `real` real data and perturbations
