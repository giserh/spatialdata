package org.openmole.spatialdata.test

object Test extends App {

  // Spatial interaction models
  //TestSpatialInteraction.testFitSinglyConstrainedRandom
  //TestSpatialInteraction.testFlowData
  //TestSpatialInteraction.testFitSinglyConstrainedSyntheticFlows
  //TestSpatialInteraction.testSyntheticFlows

  // Matrices
  //TestMatrix.testImplementations

  // Coupled network / grid
  //TestCoupled.testWeakCouplingNodePositions()
  //TestCoupled.testRandom()

  // Network algorithms
  //TestNetwork.testCoarseGraining
  //TestNetwork.testSimplification
  //TestNetwork.testCycles
  //TestNetwork.testShortestPathImplementations()
  //TestNetwork.testPlanarization()

  // Matsim
  //TestMatsim.testMatsimNetworkGenerator()

  // OSM
  //TestOSM.testBuildingExtractor()
  //TestOSM.testOSMGridSampling

  // Database
  //TestDatabase.testPostgis
  //TestDatabase.testMongo

  // Indicators
  //TestIndicators.testSpatialIndics
  //TestIndicators.testMorphology
  //TestIndicators.testStats
  //TestIndicators.testFFTConvolution

  // Synthetic networks
  //TestNetwork.testOSMNetwork
  //TestNetwork.testTreeMinDist
  //TestNetwork.testRandomNetwork

  // Reaction Diffusion
  //TestReactionDiffusion.testRealDataCalibration
  //TestReactionDiffusion.testModel
  //TestReactionDiffusion.benchmarkImplementation

  // Synthetic grids
  TestSynthetic.testCorrelatedPercolation()
  //TestSynthetic.testRandomGrids()
  //TestSynthetic.testBlocksGrid()
  //TestSynthetic.testPercolationGrid()
  //TestSynthetic.testGeneratorLauncher()
  //TestSynthetic.testGeneratorCalibration()

  // Maths
  //TestMaths.testFourierCorrelatedField()
  //TestMaths.testConvolution
  //TestMaths.testConvolution2D

  // Utils
  //TestUtils.testCSVMatrix
  //TestUtils.testBinaryIO
  //TestUtils.testSampling
  //TestUtils.testCSV
  //TestPNG.testPNG()
  //TestPNG.testOSMGridSampling()

}
