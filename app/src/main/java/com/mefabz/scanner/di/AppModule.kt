package com.mefabz.scanner.di

import com.mefabz.scanner.core.dispatcher.IoDispatcher
import com.mefabz.scanner.data.remote.OcrInvoiceDataSource
import com.mefabz.scanner.data.repository.InvoiceRepositoryImpl
import com.mefabz.scanner.domain.repository.InvoiceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideInvoiceRepository(
        dataSource: OcrInvoiceDataSource
    ): InvoiceRepository {
        return InvoiceRepositoryImpl(dataSource)
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
